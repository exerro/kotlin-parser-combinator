
// TODO: comments!

typealias P<T> = (PState) -> PResult<T>
typealias AnyP = P<Any?>

private typealias PairList<A, B> = List<Pair<A, B>>

open class PState(val lexer: Lexer) {
    val pos get() = lexer.next().first.getPosition()
    open fun consume() = PState(lexer.next().second)
}

sealed class PResult<out T>
data class POK<T>(val value: T, val state: PState): PResult<T>()
data class PFail<T>(val error: ParseError): PResult<T>()

@Suppress("ClassName", "MemberVisibilityCanBePrivate", "unused")
object parser {
    fun <T> value(value: T): P<T> = { s -> POK(value, s) }
    fun <T> error(error: ParseError): P<T> = { _ -> PFail(error) }
    fun <T> p(p: () -> P<T>): P<T> = { s -> p()(s) }
    val nothing: P<Unit> = value(Unit)
    val state: P<PState> = { s -> POK(s, s) }
    val token: P<Token> = { s -> POK(s.lexer.next().first, s.consume()) }
    val identifier = tokenType(TOKEN_IDENT)
    val integer = tokenType(TOKEN_INT)
    val number = tokenType(TOKEN_FLOAT)
    val character = tokenType(TOKEN_CHAR)
    val string = tokenType(TOKEN_STR)
    val eof = tokenType(TOKEN_EOF)
    val newline = state bind { s -> lookahead(token filter { it.getPosition().line1 > s.lexer.lastTokenPosition.line2 }) } // TODO: better error message

    fun tokenType(type: TokenType) = token err { if (it.type == type) null else ParseError("Expecting any $type, got ${it.type} ('${it.text}')", it.getPosition()) }
    fun tokenValue(type: TokenType, value: String) = token err { if (it.type == type && it.text == value) null else ParseError("Expecting $type ('$value'), got ${it.type} ('${it.text}')", it.getPosition()) }
    fun keyword(word: String) = tokenValue(TOKEN_KEYWORD, word)
    fun keywords(keywords: List<String>): P<Set<Token>> = oneOf(keywords.map(::keyword)) bind { k -> keywords(keywords.filter { it != k.text }) map { setOf(k) + it } } mapErr { value(setOf()) }
    fun keywords(vararg keywords: String): P<Set<Token>> = keywords(keywords.toList())
    fun symbol(symbol: String) = symbol.drop(1).fold(tokenValue(TOKEN_SYM, "${symbol[0]}")) { a, b -> a nextTo tokenValue(TOKEN_SYM, "$b") }
    @Deprecated("Use symbol() instead") fun sym(symbol: String) = symbol.drop(1).fold(tokenValue(TOKEN_SYM, "${symbol[0]}")) { a, b -> a nextTo tokenValue(TOKEN_SYM, "$b") }
    fun <T> lookahead(p: P<T>): P<T> = state bind { s -> p bind { { _: PState -> POK(it, s) } } }
    fun <T> wrap(p: P<T>, a: AnyP, b: AnyP): P<T> = a then p followedBy b
    fun <T> wrap(p: P<T>, a: String, b: String): P<T> = wrap(p, symbol(a), symbol(b))
    fun <T> oneOf(parsers: List<P<T>>): P<T> = { s -> parsers.map { it(s) } .let { results -> results.firstOrNull { it is POK }
            ?: PFail(ParseError("No viable alternatives", s.pos, results.map { (it as PFail).error } .toSet())) } }
    fun <T> branch(vararg options: Pair<AnyP, P<T>>): P<T> = { s -> options.map { Pair(it.first(s), it.second) } .let { results -> results.firstOrNull { it.first is POK } ?.let { it.second(s) }
            ?: PFail(ParseError("No viable alternatives", s.pos, results.map { (it.first as PFail).error } .toSet())) } } // TODO: avoid reparsing to get error
    fun <T> oneOf(vararg parsers: P<T>): P<T> = oneOf(parsers.toList())
    fun <T> optional(p: P<T>): P<T?> = p or (nothing map { null })
    fun <T> optionalList(p: P<List<T>>): P<List<T>> = optional(p) map { it ?: listOf() }
    fun <T> many(parser: P<T>): P<List<T>> = parser bind { fst -> p { many(parser) map { listOf(fst) + it } } } mapErr { value(listOf()) }
    fun <T> sequence(fn: Sequence.() -> T): P<T> = { s -> val seq = Sequence(s); try { POK(fn(seq), seq.state) } catch (e: SErr) { PFail(e.err) } }

    inline infix fun <T, R> P<T>.map(crossinline fn: (T) -> R): P<R> = bind { value -> value(fn(value)) }
    inline infix fun <T> P<T>.err(crossinline test: (T) -> ParseError?) = bind { value -> test(value)?.let(::error) ?: value(value) }
    inline infix fun <T> P<T>.filter(crossinline predicate: (T) -> Boolean) = state bind { s -> this err { if (predicate(it)) null else ParseError("Predicate failed", s.pos) } }
    infix fun <A, B> P<A>.andThen(p: P<B>): P<Pair<A, B>> = bind { f -> p map { Pair(f, it) } }
    infix fun <T> AnyP.then(p: P<T>): P<T> = bind { p }
    infix fun <T> P<T>.followedBy(p: AnyP): P<T> = bind { v -> p map { v } }
    infix fun <T> P<T>.or(p: P<T>): P<T> = state bind { s -> mapErr { err -> p mapErr { err2 -> error(ParseError("No viable alternative", s.pos, setOf(err, err2))) } } }
    infix fun <T> P<T>.until(p: AnyP): P<List<T>> = lookahead(p) map { listOf<T>() } mapErr { (this bind { f -> until(p) map { rest -> listOf(f) + rest } }) }
    infix fun <T> P<T>.sepBy(p: AnyP): P<List<T>> = bind { first -> p then sepBy(p) map { rest -> listOf(first) + rest } or value(listOf(first)) }
    infix fun P<Token>.nextTo(p: P<Token>): P<Token> = bind { first -> p err { if (it.getPosition().follows(first.getPosition())) null
        else ParseError("Token '${it.text}' doesn't follow '${first.text}'", it.getPosition()) } }
    infix fun <T> P<T>.defaultsTo(default: T): P<T> = this or value(default)
    infix fun <T, O> P<T>.sepByOp(p: P<O>): P<Pair<T, PairList<O, T>>> = bind { first -> p mapEither { operator -> when (operator) {
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
}

@Suppress("unused")
infix fun <T> P<T>.parse(s: StringTextStream) = this(PState(Lexer(s, LexerTools.defaults)))
infix fun <T> P<T>.parse(s: String) = this(PState(Lexer(StringTextStream(s), LexerTools.defaults))).getOr {
    error(it.getString { StringTextStream(s) })
}

inline infix fun <T> PResult<T>.apply(fn: (T) -> Any?): PResult<T> = when (this) { is POK -> fn(value) else -> null } .let { this }
inline infix fun <T, R> PResult<T>.map(fn: (T) -> R): R? = when (this) { is POK -> fn(value) else -> null }
inline infix fun <T> PResult<T>.onError(fn: (ParseError) -> Any?): PResult<T> = when (this) { is PFail -> fn(error) else -> null } .let { this }

fun <T> PResult<T>.get(): T? = when (this) { is POK -> value else -> null }
fun <T> PResult<T>.getOr(fn: (ParseError) -> T): T = when (this) { is POK -> value; is PFail -> fn(error) }
