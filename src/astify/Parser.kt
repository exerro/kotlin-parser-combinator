package astify

import astify.util.*
import kotlin.math.max

typealias P<Token, T> = (ParserState<Token>) -> ParseResult<Token, T>

class ParserContext<Token> {
    val nothing: P<Token, Unit> = { s -> ParseSuccess(Unit, s.position, s) }
    val any: P<Token, Token> = { s -> when (s.token) {
        null -> ParseFail("Expected token, got EOF", s.position)
        else -> ParseSuccess(s.token!!, s.position, s.next)
    } }
    val eof: P<Token, Unit> = { s -> when (s.token) {
        null -> ParseSuccess(Unit, s.position, s)
        else -> ParseFail("EOF expected", s.position)
    } }

    fun <T> inject(value: T): P<Token, T>
            = { s -> ParseSuccess(value, s.position, s) }

    fun <T> lookahead(p: P<Token, T>): P<Token, T>
            = { s -> (p fmap { ParseSuccess(it.value, it.position, s) })(s) }

    infix fun <T, R> P<Token, T>.fmap(
            fn: (ParseSuccess<Token, T>) -> ParseResult<Token, R>
    ): P<Token, R> = { s -> when (val r = this(s)) {
        is ParseSuccess -> fn(r)
        is ParseFail -> r
    } }

    infix fun <A, B> P<Token, A>.bind(fn: (A, Position) -> P<Token, B>): P<Token, B>
            = fmap { fn(it.value, it.position)(it.nextState) }

    infix fun <A, B> P<Token, A>.bindv(fn: (A) -> P<Token, B>): P<Token, B>
            = fmap { fn(it.value)(it.nextState) }

    infix fun <A, B> P<Token, A>.map(fn: (A, Position) -> ValuePositionPair<B>): P<Token, B>
            = fmap { val (v, p) = fn(it.value, it.position); ParseSuccess(v, p, it.nextState) }

    infix fun <A, B> P<Token, A>.mapv(fn: (A) -> B): P<Token, B>
            = fmap { ParseSuccess(fn(it.value), it.position, it.nextState) }

    fun <T> P<Token, T>.satisfying(error: String, fn: (T) -> Boolean): P<Token, T>
            = fmap { r -> if (fn(r.value)) r else ParseFail(error, r.position) }

    infix fun <T> P<Token, T>.satisfying(fn: (T) -> Boolean): P<Token, T>
            = satisfying("Failed predicate", fn)

    infix fun <T> P<Token, T>.equalTo(value: T): P<Token, T>
            = satisfying("Expected <$value>") { it == value }

    ////////////////////////////////////////////////////////////////////////////

    infix fun <A, B> P<Token, A>.then(p: P<Token, B>): P<Token, Pair<A, B>>
            = bind { a, ap -> p map { b, bp -> (a to b) positioned ap.to(bp) } }

    infix fun <T> P<Token, T>.proceededBy(p: P<Token, *>): P<Token, T>
            = bind { a, ap -> p map { _, _ -> a positioned ap } }

    infix fun <T> P<Token, T>.preceededBy(p: P<Token, *>): P<Token, T>
            = p bind { _, _ -> this map { b, bp -> b positioned bp } }

    infix fun <T> P<Token, T>.sepBy(p: P<Token, *>): P<Token, List<T>> = bindv { value -> branch(
            p to (this sepBy p preceededBy p mapv { listOf(value) + it }),
            nothing to inject(listOf(value))
    ) }

    infix fun <T> P<Token, T>.or(p: P<Token, T>): P<Token, T> = { s -> when (val r = this(s)) {
        is ParseSuccess -> r
        else -> when (val rr = p(s)) {
            is ParseSuccess -> rr
            else -> ParseFail("No viable alternatives", s.position,
                    listOf(r as ParseFail, rr as ParseFail))
        }
    } }

    infix fun <R, T: R> P<Token, T?>.orElse(p: P<Token, R>)
            = this bindv { if (it == null) p else inject(it) }

    fun <T> oneOf(vararg parsers: P<Token, T>): P<Token, T> = { s ->
        val results = LazyMappedList(parsers.toList()) { p -> p(s) }

        results.first { it is ParseSuccess }
                ?: ParseFail("No viable alternatives", s.position,
                        results.all().map { it as ParseFail })
    }

    fun <A, B> either(a: P<Token, A>, b: P<Token, B>): P<Token, Either<A, B>> = { s -> when (val r = a(s)) {
        is ParseSuccess -> ParseSuccess(Either.Left(r.value), r.position, r.nextState)
        else -> when (val rr = b(s)) {
            is ParseSuccess -> ParseSuccess(Either.Right(rr.value), rr.position, rr.nextState)
            else -> ParseFail("No viable alternatives", s.position,
                    listOf(r as ParseFail, rr as ParseFail))
        }
    } }

    fun <T> branch(vararg parsers: Pair<P<Token, *>, P<Token, T>>): P<Token, T> = { s ->
        val results = LazyMappedList(parsers.toList()) { (match, p) -> when (val r = p(s)) {
            is ParseSuccess -> true to p(s)
            is ParseFail -> false to r
        } }

        results.first { it.first } ?.second
                ?: ParseFail("No viable alternatives", s.position,
                        results.all().map { it.second as ParseFail })
    }

    fun <T> lazy(fn: () -> P<Token, T>): P<Token, T>
            = { s -> fn()(s) }

    fun <T> wrap(p: P<Token, T>, pre: P<Token, *>, post: P<Token, *> = pre)
            = p preceededBy pre proceededBy post

    fun <T> optional(p: P<Token, T>): P<Token, T?> = { s -> when (val r = p(s)) {
        is ParseSuccess -> r
        is ParseFail -> ParseSuccess(null, s.position, s)
    } }

    fun <T> many(p: P<Token, T>): P<Token, List<T>> = optional(p) bind { x, pos ->
        if (x == null) inject(listOf())
        else many(p) map { xs, ps -> listOf(x) + xs positioned (pos.to(ps.after(-1))) }
    }

    fun <T> many(limit: Int, p: P<Token, T>): P<Token, List<T>> = when (max(0, limit)) {
        0 -> many(p)
        else -> p bind { x, xp -> many(limit - 1, p) map { xs, xsp ->
            listOf(x) + xs positioned xp.to(xsp) } }
    }

    fun <T> sequence(fn: SequenceParserContext<Token>.() -> ValuePositionPair<T>): P<Token, T> = { s ->
        SequenceParserContext(s).apply(fn)
    }

    ////////////////////////////////////////////////////////////////////////////

    fun positionOf(p: P<Token, *>): P<Token, Position> = p map { _, pos -> pos positioned pos }
}

fun <Token, T> parser2(fn: ParserContext<Token>.() -> P<Token, T>): P<Token, T> = fn(ParserContext())

//////////////////////////////////////////////////////////////////////////////////////////

fun main() {
    val str = TextStream("52.3 2819")
    val parser = parser2<Char, Any?> { many(eof) }
    val result = parser(TextStreamParserState(str, Position(0)))

    when (result) {
        is ParseSuccess -> println("Parsed ${result.value}\n${result.position.linePointer(str)}")
        is ParseFail -> println("Error: ${result.error}\n${result.position.linePointer(str)}")
    }

    val a: Either<Int, String> = Either.Left(5)
    val b: Either<Int, String> = Either.Right("hello")
}
