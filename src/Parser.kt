
// TODO: comments!

typealias P<T> = (PState) -> PResult<T>
typealias AnyP = P<Any?>

private typealias PairList<A, B> = List<Pair<A, B>>

open class PState(val lexer: Lexer) {
    val pos get() = lexer.nextToken.getPosition()
    open fun consume() = PState(lexer.nextLexer)
}

sealed class PResult<out T>
data class POK<T>(val value: T, val state: PState): PResult<T>()
data class PFail<T>(val error: ParseError): PResult<T>()

@Suppress("ClassName", "MemberVisibilityCanBePrivate", "unused")
object parser {
    val nothing: P<Unit> = value(Unit)
    val state: P<PState> = { s -> POK(s, s) }
    val peek: P<Token> = { s -> POK(s.lexer.nextToken, s) }
    val token: P<Token> = { s -> POK(s.lexer.nextToken, s.consume()) }
    val identifier: P<Token> = tokenType(TOKEN_IDENT)
    val integer: P<Token> = tokenType(TOKEN_INT)
    val number: P<Token> = tokenType(TOKEN_FLOAT)
    val character: P<Token> = tokenType(TOKEN_CHAR)
    val string: P<Token> = tokenType(TOKEN_STR)
    val eof: P<Token> = tokenType(TOKEN_EOF)
    val newline: P<Token> = state bind { s ->
        lookahead(token filter { it.getPosition().line1 > s.lexer.lastTokenPosition.line2 }) or error("Expecting newline before character", s.pos)
    }

    fun <T> value(value: T): P<T>
            = { s -> POK(value, s) }
    fun <T> error(error: ParseError): P<T>
            = { _ -> PFail(error) }
    fun <T> error(error: String, pos: Position): P<T>
            = { _ -> PFail(ParseError(error, pos)) }
    fun <T> p(p: () -> P<T>): P<T>
            = { s -> p()(s) }
    fun <T> pos(p: P<T>): P<Positioned<T>>
            = state bind { s -> (p andThen state) map { (value, ss) -> positioned(value, s.pos to ss.lexer.lastTokenPosition) } }
    fun tokenType(type: TokenType): P<Token>
            = peek bind { t -> token filter { t.type == type } or error("Expecting any $type, got ${t.type} ('${t.text}')", t.getPosition()) }
    fun tokenValue(type: TokenType, value: String): P<Token>
            = peek bind { t -> token filter { t.type == type && t.text == value } or error("Expecting $type ('$value'), got ${t.type} ('${t.text}')", t.getPosition()) }
    fun keyword(word: String): P<Token>
            = tokenValue(TOKEN_KEYWORD, word)
    fun keywords(keywords: List<String>): P<Set<Token>>
            = oneOf(keywords.map(::keyword)) bind { k -> keywords(keywords.filter { it != k.text }) map { setOf(k) + it } } mapErr { value(setOf()) }
    fun keywords(vararg keywords: String): P<Set<Token>>
            = keywords(keywords.toList())
    fun identifier(word: String): P<Token>
            = tokenValue(TOKEN_IDENT, word)
    fun symbol(symbol: String): P<Token>
            = symbol.drop(1).fold(tokenValue(TOKEN_SYM, "${symbol[0]}")) { a, b -> a nextTo tokenValue(TOKEN_SYM, "$b") }
    fun <T> nothing(): P<T?>
            = nothing map { null }
    fun <T> sequence(fn: Sequence.() -> T): P<T>
            = { s -> val seq = Sequence(s); try { POK(fn(seq), seq.state) } catch (e: SErr) { PFail(e.err) } }

    fun text(p: P<Token>): P<String>
            = p map { it.text }
    fun <T> lookahead(p: P<T>): P<T>
            = state bind { s -> p bind { result -> { _: PState -> POK(result, s) } } }
    fun <T> wrap(p: P<T>, a: AnyP, b: AnyP): P<T>
            = a then p followedBy b
    fun <T> wrap(p: P<T>, a: String, b: String): P<T>
            = wrap(p, symbol(a), symbol(b))
    fun <T> branch(vararg options: Pair<AnyP, P<T>>): P<T>
            = { s -> options.map { Pair(it.first(s), it.second) } .let { results -> results.firstOrNull { it.first is POK } ?.let { it.second(s) }
            ?: PFail(ParseError("No viable alternatives", s.pos, results.map { (it.first as PFail).error } .toSet())) } }
    fun <T> oneOf(vararg parsers: P<T>): P<T>
            = oneOf(parsers.toList())
    fun <T> oneOf(parsers: List<P<T>>): P<T>
            = { s -> parsers.map { it(s) } .let { results -> results.firstOrNull { it is POK }
            ?: PFail(ParseError("No viable alternatives", s.pos, results.map { (it as PFail).error } .toSet())) } }
    fun <T> optional(p: P<T>): P<T?>
            = p mapErr { nothing<T>() }
    fun <T> many(p: P<T>): P<List<T>>
            = p bind { fst -> many(p) map { listOf(fst) + it } } mapErr { value(listOf()) }

    inline infix fun <T, R> P<T>.map(crossinline fn: (T) -> R): P<R>
            = bind { value -> value(fn(value)) }
    inline infix fun <T> P<T>.err(crossinline test: (T) -> ParseError?): P<T>
            = bind { value -> test(value)?.let(::error) ?: value(value) }
    inline infix fun <T> P<T>.filter(crossinline predicate: (T) -> Boolean): P<T?>
            = map { if (predicate(it)) it else null }
    infix fun <T> AnyP.ifThen(p: P<T>): P<T?>
            = lookahead(this) mapEither { when (it) { is POK -> p else -> nothing<T>() } }
    infix fun <T> AnyP.ifNotThen(p: P<T>): P<T?>
            = lookahead(this) mapEither { when (it) { is POK -> nothing<T>() else -> p } }
    infix fun <T> P<T?>.defaultsTo(value: T): P<T>
            = map { it ?: value }
    infix fun <T> P<T?>.or(p: P<T>): P<T>
            = bind { if (it != null) value(it) else p }
    infix fun <T> AnyP.then(p: P<T>): P<T>
            = bind { p }
    infix fun <A, B> P<A>.andThen(p: P<B>): P<Pair<A, B>>
            = bind { f -> p map { Pair(f, it) } }
    infix fun <T> P<T>.followedBy(p: AnyP): P<T>
            = bind { v -> p map { v } }
    infix fun <T> P<T>.until(p: AnyP): P<List<T>>
            = lookahead(p) map { listOf<T>() } mapErr { (this bind { f -> until(p) map { rest -> listOf(f) + rest } }) }
    infix fun <T> P<T>.sepBy(p: AnyP): P<List<T>>
            = bind { first -> branch(p to (p then sepBy(p) map { rest -> listOf(first) + rest }), nothing to value(listOf(first))) }
    infix fun P<Token>.nextTo(pa: P<Token>): P<Token>
            = bind { first -> pa bind { second ->
                if (second.getPosition() follows first.getPosition())
                    value(Token(TOKEN_SYM, first.text + second.text, first.getPosition() to second.getPosition()))
                else
                    error("Unexpected whitespace between ('${first.text}') and ('${second.text}')") } }
    infix fun <T, O> P<T>.sepByOp(p: P<O>): P<Pair<T, PairList<O, T>>>
            = bind { first -> p mapEither { operator -> when (operator) {
                is POK -> sepByOp(p) map { (n, ops) -> Pair(first, listOf(Pair(operator.value, n)) + ops) }
                is PFail -> value(Pair(first, listOf()))
            } } }

    inline infix fun <T, R> P<T>.mapEither(crossinline fn: (PResult<T>) -> P<R>): P<R> = { s -> when (val result = this(s)) {
        is POK -> fn(result)(result.state)
        is PFail -> fn(result)(s)
    } }

    inline infix fun <T, R> P<T>.bind(crossinline fn: (T) -> P<R>): P<R> = { s -> when (val result = this(s)) {
        is POK -> fn(result.value)(result.state)
        is PFail -> PFail(result.error)
    } }

    inline infix fun <T> P<T>.mapErr(crossinline fn: (ParseError) -> P<T>): P<T> = { s -> when (val result = this(s)) {
        is POK -> result
        is PFail -> fn(result.error)(s)
    } }

    operator fun <T> invoke(fn: parser.() -> P<T>): P<T> = fn(parser)

    class Sequence internal constructor(var state: PState) {
        fun <T> p(getParser: parser.() -> P<T>): T = when (val result = getParser(parser)(state)) {
            is POK -> { state = result.state; result.value }
            is PFail -> throw SErr(result.error)
        }
    }

    private class SErr(val err: ParseError): Throwable()

    // TODO: remove deprecated functions
    @Deprecated("Generates bad error messages.", ReplaceWith("symbol(\"endSymbol\") ifNotThen p defaultsTo listOf()"))
    fun <T> optionalList(p: P<List<T>>): P<List<T>> = optional(p) defaultsTo listOf()

    @Deprecated("Use symbol() instead", ReplaceWith("symbol(symbol)"))
    fun sym(symbol: String) = symbol.drop(1).fold(tokenValue(TOKEN_SYM, "${symbol[0]}")) { a, b -> a nextTo tokenValue(TOKEN_SYM, "$b") }
}

@Suppress("unused")
infix fun <T> P<T>.parse(s: StringTextStream) = this(PState(Lexer(s, LexerTools.defaults)))
infix fun <T> P<T>.parse(s: String) = this(PState(Lexer(StringTextStream(s), LexerTools.defaults))).getOr {
    error(it.getString(StringTextStream(s)))
}

@Suppress("unused")
inline infix fun <T> PResult<T>.apply(fn: (T) -> Any?): PResult<T>
        = when (this) { is POK -> fn(value) else -> null } .let { this }

@Suppress("unused")
inline infix fun <T, R> PResult<T>.map(fn: (T) -> R): R?
        = when (this) { is POK -> fn(value) else -> null }

@Suppress("unused")
inline infix fun <T> PResult<T>.onError(fn: (ParseError) -> Any?): PResult<T>
        = when (this) { is PFail -> fn(error) else -> null } .let { this }

@SuppressWarnings("unused")
infix fun <T> PResult<T>.getOrError(stream: TextStream): T
        = when (this) { is POK -> value; is PFail -> error(error.getString(stream)) }

infix fun <T> PResult<T>.getOr(fn: (ParseError) -> T): T
        = when (this) { is POK -> value; is PFail -> fn(error) }

fun <T> PResult<T>.get(): T?
        = when (this) { is POK -> value else -> null }
