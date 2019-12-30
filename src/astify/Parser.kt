package astify

import astify.util.Either
import astify.util.Either.Left
import astify.util.Either.Right
import astify.util.LazyMappedList
import astify.util.PositionedValue
import astify.util.positioned

//////////////////////////////////////////////////////////////////////////////////////////

/** Create a parser. */
@Suppress("FunctionName")
fun <Token, T> P(fn: Parser<Token>.() -> P<Token, T>) = fn(Parser())

/** Define a parser. */
class P<Token, out T> private constructor(val parse: (ParserState<Token>) -> ParseResult<Token, T>) {
    companion object {
        /** Create a parser. */
        fun <Token, T> new(fn: (ParserState<Token>) -> ParseResult<Token, T>): P<Token, T>
                = P(fn)

        /** Parse a sequence of items. */
        fun <Token, T> seq(fn: ParserSequence<Token>.() -> T): P<Token, T>
                = P { s -> ParserSequence(s).run(fn) }
    }
}

//////////////////////////////////////////////////////////////////////////////////////////

open class ParserSequence<Token>(private var state: ParserState<Token>): Parser<Token>() {
    operator fun <T> P<Token, T>.component1(): T {
        when (val r = this.parse(state)) {
            is ParseSuccess -> {
                state = r.nextState
                return r.value
            }
            is ParseFail -> throw PSeqError(r)
        }
    }

    fun parse(p: P<Token, *>): Boolean = when (val r = p.parse(state)) {
        is ParseSuccess -> {
            state = r.nextState
            true
        }
        is ParseFail -> false
    }

    internal fun <T> run(fn: ParserSequence<Token>.() -> T): ParseResult<Token, T> = try {
        val r = fn(this)
        ParseSuccess(r, state)
    }
    catch (e: PSeqError) {
        e.err
    }

    private class PSeqError(val err: ParseFail): Throwable()
}

//////////////////////////////////////////////////////////////////////////////////////////

open class Parser<Token> {
    /** Consume no input and always succeed. */
    val nothing: P<Token, Unit> = P.new { s -> ParseSuccess(Unit, s) }
    /** Match any token and yield a succeed if any token was consumed. */
    val anything: P<Token, Token> = P.new { s ->
        when (val token = s.token) {
            null -> ParseFail("Expected any token", s.position)
            else -> ParseSuccess(token, s.nextState)
        }
    }
    /** Match only the end of the input stream and succeed if the end was reached. */
    val eof: P<Token, Unit> = P.new { s ->
        when (s.token) {
            null -> ParseSuccess(Unit, s)
            else -> ParseFail("Expected EOF", s.position)
        }
    }
    val newline: P<Token, Unit> = P.new { s ->
        if (s.position.line1(s.stream) > s.lastPosition.line2(s.stream)) ParseSuccess(Unit, s)
        else ParseFail("Expected token on next line", s.position)
    }
    val sameLine: P<Token, Unit> = P.new { s ->
        if (s.position.line1(s.stream) == s.lastPosition.line2(s.stream)) ParseSuccess(Unit, s)
        else ParseFail("Expected token on same line", s.position)
    }
    /** Consume no input and yield `fn()` as a parse value.
     *
     *  P { value(x) } will always succeed and have a value of x. */
    fun <T> value(fn: () -> T): P<Token, T>
            = P.new { s -> ParseSuccess(fn(), s) }
    /** Consume no input and yield `fn()` as a parse value.
     *
     *  P { value(x) } will always succeed and have a value of x. */
    fun <T> value(value: T, position: Position): P<Token, T>
            = P.new { s -> ParseSuccess(value, s) }

    fun satisfying(condition: (Token) -> Boolean): P<Token, Token> = positioned(anything) fmap {
        if (condition(it.value.value)) it.fmap { p -> p.value }
        else ParseFail("Predicate failed", it.value.position)
    }

    fun equalTo(value: Token): P<Token, Token>
            // TODO: error format function parameter?
            = satisfying { it == value } fmape { e, _ -> e.copy(error = "Expected token '$value'") }

    inline fun <reified R> convertType(
            typename: String? = R::class.simpleName
    // TODO: error format function parameter?
    ): P<Token, R> = satisfying { it is R } fmape { f, _ ->
        f.copy(error = "Expected $typename")
    } map { it as R }

    // Support functions ///////////////////////////////////////////////////////

    /** Map a parse success to a result, or leave a parse fail as-is. */
    infix fun <T, R> P<Token, T>.fmap(
            fn: (ParseSuccess<Token, T>) -> ParseResult<Token, R>
    ): P<Token, R> = P.new { s ->
        when (val r = this.parse(s)) {
            is ParseSuccess -> fn(r)
            is ParseFail -> r
        }
    }

    /** Map a parse fail to a result, leaving a parse success as-is. */
    infix fun <T> P<Token, T>.fmape(
            fn: (ParseFail, ParserState<Token>) -> ParseResult<Token, T>
    ): P<Token, T> = P.new { s ->
        when (val r = this.parse(s)) {
            is ParseSuccess -> r
            is ParseFail -> fn(r, s)
        }
    }

    /** Map a parse success to another parser, which is in turn evaluated after
     *  `this`. */
    infix fun <T, R> P<Token, T>.flatMap(
            fn: (ParseSuccess<Token, T>) -> P<Token, R>
    ): P<Token, R> = P.new { s ->
        when (val r = this.parse(s)) {
            is ParseSuccess -> fn(r).parse(r.nextState)
            is ParseFail -> r
        }
    }

    /** Map a parse fail to another parser, which is in turn evaluated with the
     *  same state as `this`. */
    infix fun <T> P<Token, T>.flatMapE(
            fn: (ParseFail, ParserState<Token>) -> P<Token, T>
    ): P<Token, T> = P.new { s ->
        when (val r = this.parse(s)) {
            is ParseSuccess -> r
            is ParseFail -> fn(r, s).parse(s)
        }
    }

    /** Map the value of a parse success to another value. */
    infix fun <T, R> P<Token, T>.map(fn: (T) -> R): P<Token, R>
            = fmap { it.fmap(fn) }

    /** Map the value of a parse success to another parser, and evaluate that
     *  parser after `this`. */
    infix fun <T, R> P<Token, T>.bind(fn: (T) -> P<Token, R>): P<Token, R>
            = flatMap { fn(it.value) }

    // Operators ///////////////////////////////////////////////////////////////

    /** Lazily evaluate a parser, useful for cyclic references. */
    fun <T> lazy(fn: () -> P<Token, T>): P<Token, T> {
        lateinit var p: P<Token, T>
        var evaluated = false
        return P.new { s ->
            if (!evaluated) {
                p = fn(); evaluated = true
            }
            p.parse(s)
        }
    }

    /** Extract the position of a parse result, wrapping its value as a
     *  PositionedValue.
     *
     *  P { positioned(anything) }: PositionedValue<Token> */
    fun <T> positioned(p: P<Token, T>): P<Token, PositionedValue<T>> = P.new { s ->
        when (val r = p.parse(s)) {
            is ParseSuccess -> r.fmap { it positioned (s.position to r.nextState.lastPosition) }
            is ParseFail -> r
        }
    }

    /** Try to parse `p`, but return `null` if the parse fails. */
    fun <T> optional(p: P<Token, T>): P<Token, T?>
            = p map { it as T? } fmape { _, s -> ParseSuccess(null, s) }

    /** Parse 0 or more occurrences of `p`. */
    fun <T> many(p: P<Token, T>): P<Token, List<T>> = P.new { s ->
        when (val r = p.parse(s)) {
            is ParseSuccess -> {
                val rs = (many(p).parse(r.nextState) as ParseSuccess)
                ParseSuccess(listOf(r.value) + rs.value, rs.nextState)
            }
            is ParseFail -> ParseSuccess(listOf(), s)
        }
    }

    /** Parse `n` or more occurrences of `p`. */
    fun <T> many(n: Int, p: P<Token, T>): P<Token, List<T>> = when {
        n <= 0 -> many(p)
        else -> p flatMap { r -> many(n - 1, p) fmap { rs ->
            ParseSuccess(listOf(r.value) + rs.value, rs.nextState)
        } }
    }

    /** Return using the first of the parsers given that matches successfully,
     *  or fail with a message representative of all the errors. */
    fun <T> oneOf(ps: List<P<Token, T>>): P<Token, T> = P.new { s ->
        val parsers = LazyMappedList(ps) {
            it.parse(s)
        }

        parsers.first { it is ParseSuccess }
                ?.let { it as ParseSuccess }
                ?: ParseFail("No viable alternatives", s.position,
                        parsers.all().map { it as ParseFail })
    }

    /** Return using the first of the parsers given that matches successfully,
     *  or fail with a message representative of all the errors. */
    fun <T> oneOf(vararg p: P<Token, T>): P<Token, T> = oneOf(p.toList())

    fun <T> branch(vararg p: Pair<P<Token, *>, P<Token, T>>): P<Token, T> = P.new { s ->
        val parsers = LazyMappedList(p.toList()) { (match, p) ->
            when (val r = match.parse(s)) {
                is ParseSuccess -> true to p.parse(s)
                is ParseFail -> false to r
            }
        }

        parsers.first { it.first }?.second
                ?: ParseFail("No viable alternatives", s.position,
                        parsers.all().map { it.second as ParseFail })
    }

    // Binary operators ////////////////////////////////////////////////////////

    infix fun <T> P<Token, PositionedValue<T>>.satisfying(
            condition: (T) -> Boolean
    ): P<Token, PositionedValue<T>> = fmap {
        if (condition(it.value.value)) it
        else ParseFail("Predicate failed", it.value.position)
    }

    // TODO: equalTo

    infix fun <A, B> P<Token, A>.and(p: P<Token, B>): P<Token, Pair<A, B>>
            = flatMap { a -> p fmap {
        ParseSuccess(
                a.value to it.value, it.nextState)
    } }

    infix fun <T> P<Token, *>.enables(p: P<Token, T>): P<Token, T?>
            = P.new { s ->
        when (val r = this.parse(s)) {
            is ParseSuccess -> p.parse(r.nextState)
            is ParseFail -> ParseSuccess(null, s)
        }
    }

    infix fun <T> P<Token, T>.keepLeft(p: P<Token, *>): P<Token, T>
            = flatMap { v -> p fmap { ParseSuccess(v.value, it.nextState) } }

    infix fun <T> P<Token, *>.keepRight(p: P<Token, T>): P<Token, T>
            = bind { p }

    infix fun <T> P<Token, T>.sepBy(p: P<Token, *>): P<Token, List<T>> = let { self ->
        P.seq {
            val (r) = self
            val results = mutableListOf(r)

            while (parse(p)) {
                val (next) = self
                results.add(next)
            }

            results
        }
    }

    // TODO: this shouldn't really be a thing
    //       it exists because (a, b) and () might want to be parsed
    //       however, in that case, there's always a closing entity that should
    //       be used to decide whether to parse the sepBy
    infix fun <T> P<Token, T>.sepBy0(p: P<Token, *>): P<Token, List<T>>
            = this enables (this sepBy p) orElse value { listOf<T>() } // TODO: this is inefficient

    infix fun <A, B> P<Token, A>.orEither(p: P<Token, B>): P<Token, Either<A, B>>
            = map { Left(it) } flatMapE { _, _ -> p map { Right(it) } }

    infix fun <T> P<Token, T>.or(p: P<Token, T>): P<Token, T>
            = flatMapE { _, _ -> p }

    infix fun <R, T: R> P<Token, T?>.orElse(p: P<Token, R>): P<Token, R> = flatMap { r ->
        if (r.value == null) p else P.new { ParseSuccess(r.value, r.nextState) }
    }

    infix fun <T> P<Token, T>.until(p: P<Token, *>): P<Token, List<T>> = let { self ->
        P.seq {
            val results = mutableListOf<T>()

            while (!parse(p)) {
                val (r) = self
                results.add(r)
            }

            results
        }
    }

    // Shorthand utility operators /////////////////////////////////////////////

    fun <T> wrap(p: P<Token, T>, l: P<Token, *>, r: P<Token, *> = l): P<Token, T>
            = l keepRight p keepLeft r
}
