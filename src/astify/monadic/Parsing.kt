package astify.monadic

import astify.util.Either

////////////////////////////////////////////////////////////////////////////////
/// Parsing context objects and sequence mechanics                           ///
////////////////////////////////////////////////////////////////////////////////

/** A context wrapper to provide information about state and errors.
 *  Instantiated automatically in `p`. */
open class Parsing<State: ParserState<State>, Error> internal constructor()

/** A utility class used for creating monadic do-notation style parsers. */
open class SequenceParsing<State: ParserState<State>, Error> internal constructor(private var state: State) {
    /** Parse and update internal state of the sequence parser. Throws if parse
     *  failed. */
    operator fun <Value> P<State, Error, Value>.component1(): Value = when (val v = parse(state)) {
        is ParseResult.Success -> {
            state = v.state
            v.value
        }
        is ParseResult.Failure -> throw SequenceParsingError(this@SequenceParsing, v)
    }

    /** Return true and update state if the parser parses, otherwise return
     *  false. */
    fun <Value> P<State, Error, Value>.parses(): Boolean = when (val v = parse(state)) {
        is ParseResult.Success -> {
            state = v.state
            true
        }
        is ParseResult.Failure -> false
    }

    internal fun <Value> apply(
            fn: SequenceParsing<State, Error>.() -> Value
    ): ParseResult<State, Error, Value> = try {
        ParseResult.Success(fn(), state)
    }
    catch (e: SequenceParsingError) {
        // this can only be thrown from above, from within this instance,
        // where the type is definitely correct
        assert(e.owner == this) // just incase!
        @Suppress("UNCHECKED_CAST")
        e.e as ParseResult.Failure<State, Error>
    }

    private class SequenceParsingError(
            val owner: SequenceParsing<*, *>,
            val e: ParseResult.Failure<*, *>
    ): Throwable()
}

/** Create a parser. Used for context wrapping. */
fun <State: ParserState<State>, Error, Value> p(
        fn: Parsing<State, Error>.() -> P<State, Error, Value>
) = fn(Parsing())

/** Create a sequence parser. */
fun <State: ParserState<State>, Error, Value> pseq(
        fn: SequenceParsing<State, Error>.() -> Value
): P<State, Error, Value> = P { s ->
    SequenceParsing<State, Error>(s).apply(fn)
}

////////////////////////////////////////////////////////////////////////////////
/// Generators parsing literals                                              ///
////////////////////////////////////////////////////////////////////////////////

/** Consume no input and yield Unit as a result. Never fails. */
val <State: ParserState<State>, Error> Parsing<State, Error>.
        nothing: P<State, Error, Unit> get() = P.pure(Unit)

/** Consume a token and yield that as its result. Fails if no token was
 *  available to consume. */
val <State: TokenParserState<State, Token>, Token> Parsing<State, String>.
        anything: P<State, String, Token>
    get() = P { s -> when (val t = s.token) {
        null -> ParseResult.Failure("expected token, got EOF", s)
        else -> ParseResult.Success(t, s.next)
    } }

/** Fail if a token is available to consume, or yield Unit otherwise. */
val <Token, State: TokenParserState<State, Token>> Parsing<State, String>.
        eof: P<State, String, Unit>
    get() = P { s -> when (s.token) {
        null -> ParseResult.Success(Unit, s)
        else -> ParseResult.Failure("expected EOF, got token", s)
    } }

/** Consume no input and yield `value` as a result. Never fails. */
fun <State: ParserState<State>, Error, Value> Parsing<State, Error>.
        pure(value: Value): P<State, Error, Value> = P.pure(value)

/** Lazily evaluate a parser and apply it. */
fun <State: ParserState<State>, Error, Value> lazy(
        fn: () -> P<State, Error, Value>
): P<State, Error, Value> {
    lateinit var p: P<State, Error, Value>
    var evaluated = false
    return P { s ->
        if (!evaluated) { p = fn(); evaluated = true }
        p.parse(s)
    }
}

/** Parse using the parser and consume no input. */
fun <State: ParserState<State>, Error, Value> peek(
        p: P<State, Error, Value>
): P<State, Error, Value> = P { s -> when (val r = p.parse(s)) {
    is ParseResult.Success -> ParseResult.Success(r.value, s)
    is ParseResult.Failure -> r
} }

////////////////////////////////////////////////////////////////////////////////
/// Branching parsers                                                        ///
////////////////////////////////////////////////////////////////////////////////

/** Parse either `l` or `r`. If `l` succeeds, its value will be yielded.
 *  Otherwise, 'r' will be parsed, and `l`'s error will be ignored. */
infix fun <State: ParserState<State>, Error, T> P<State, Error, T>.defaultsTo(
        p: P<State, Error, T>
): P<State, Error, T> = P { s -> when (val r = this.parse(s)) {
    is ParseResult.Success -> r
    is ParseResult.Failure -> p.parse(s)
} }

/** Parse either `l` or `r`. If `l` consumes input and then fails, that failure
 *  will be used. Otherwise, if `r` consumes input and then fails, that failure
 *  will be used. Otherwise, both failures will be reported. */
fun <State: ParserState<State>, Error, A, B> either(
        err: (String, State) -> Error,
        l: P<State, Error, A>,
        r: P<State, Error, B>
): P<State, Error, Either<A, B>> = P { state ->
    when (val lv = l.parse(state)) {
        is ParseResult.Success -> lv.withValue(Either.Left(lv.value))
        is ParseResult.Failure -> {
            if (lv.state != state) return@P lv
            when (val rv = r.parse(state)) {
                is ParseResult.Success -> rv.withValue(Either.Right(rv.value))
                is ParseResult.Failure -> {
                    if (rv.state != state) return@P rv
                    ParseResult.Failure(err("no viable alternatives", state), state, listOf(lv, rv))
                }
            }
        }
    }
}

/** Parse using one of the parsers given. The first successful parse will be
 *  used. If a parse fails, and has consumed input, that failure will be
 *  reported. Otherwise, all failures will be reported. */
fun <State: ParserState<State>, Error, Value> oneOf(
        err: (String, State) -> Error,
        parsers: Iterable<P<State, Error, Value>>
): P<State, Error, Value> = P { state ->
    val errors = mutableListOf<ParseResult.Failure<State, Error>>()

    for (p in parsers) {
        when (val v = p.parse(state)) {
            is ParseResult.Success -> return@P v
            is ParseResult.Failure -> {
                if (v.state != state) return@P v
                errors.add(v)
            }
        }
    }

    ParseResult.Failure(err("no viable alternatives", state), state, errors)
}

////////////////////////////////////////////////////////////////////////////////
/// Mutating operators                                                       ///
////////////////////////////////////////////////////////////////////////////////

/** Fail if the parser yields a result that does not satisfy the predicate.
 *  The failure is reported at the start of the parser's input. */
infix fun <State: ParserState<State>, Value> P<State, String, Value>.satisfying(
        fn: (Value) -> Boolean
): P<State, String, Value> = P { state ->
    when (val v = parse(state)) {
        is ParseResult.Success -> {
            if (fn(v.value)) v
            else ParseResult.Failure("failed predicate", state)
        }
        is ParseResult.Failure -> v
    }
}

/** Yield `null` if the parser fails and consumes no input. */
fun <State: ParserState<State>, Error, Value> optional(
        p: P<State, Error, Value>
): P<State, Error, Value?> = P { state ->
    when (val v = p.parse(state)) {
        is ParseResult.Success -> v
        is ParseResult.Failure -> {
            if (v.state != state) v
            else ParseResult.Success(null, state)
        }
    }
}

////////////////////////////////////////////////////////////////////////////////
/// Compound operators                                                       ///
////////////////////////////////////////////////////////////////////////////////

/** Greedily parse a list of `this` until an occurrence of `terminator` is
 *  parsed. Consumes `terminator`. */
fun <State: ParserState<State>, Error, Value> many(
        p: P<State, Error, Value>
): P<State, Error, List<Value>> = optional(p map { listOf(it) }) flatMap { x ->
    if (x == null) P.pure(listOf()) else many(p) map { xs -> x + xs } }

/** Parse `this` and then `r`, then yield the pair of values parsed. */
infix fun <State: ParserState<State>, Error, A, B> P<State, Error, A>.and(
        r: P<State, Error, B>
): P<State, Error, Pair<A, B>> = flatMap { a -> r map { b -> a to b } }

/** Parse `this` and then `r`, then yield the first value parsed. */
infix fun <State: ParserState<State>, Error, Value> P<State, Error, Value>.keepLeft(
        r: P<State, Error, *>
): P<State, Error, Value> = flatMap { a -> r map { a } }

/** Parse `this` and then `r`, then yield the second value parsed. */
infix fun <State: ParserState<State>, Error, Value> P<State, Error, *>.keepRight(
        r: P<State, Error, Value>
): P<State, Error, Value> = flatMap { _ -> r }

/** Greedily parse a list of `this` separated by occurrences of `sep`. */
infix fun <State: ParserState<State>, Error, Value> P<State, Error, Value>.sepBy(
        sep: P<State, Error, *>
): P<State, Error, List<Value>> = this.let { p -> pseq {
    val (init) = p
    val entries = mutableListOf(init)

    while (sep.parses()) {
        val (next) = p
        entries.add(next)
    }

    entries
} }

/** Greedily parse a list of `this` until an occurrence of `terminator` is
 *  parsed. Consumes `terminator`. */
infix fun <State: ParserState<State>, Error, Value> P<State, Error, Value>.until(
        terminator: P<State, Error, *>
): P<State, Error, List<Value>> = this.let { p -> pseq {
    val entries = mutableListOf<Value>()

    while (!terminator.parses()) {
        val (next) = p
        entries.add(next)
    }

    entries
} }

////////////////////////////////////////////////////////////////////////////////
/// Utilities using the definitions above                                    ///
////////////////////////////////////////////////////////////////////////////////

/** Parse using `p` if `this` yields `null`. */
infix fun <State: ParserState<State>, Error, Value: R, R> P<State, Error, Value?>.
orElse(p: P<State, Error, Value>): P<State, Error, R> = flatMap { v -> when (v) {
    null -> p
    else -> P.pure(v)
} }

/** Parse either `l` or `r`. If `l` consumes input and then fails, that failure
 *  will be used. Otherwise, if `r` consumes input and then fails, that failure
 *  will be used. Otherwise, both failures will be reported. */
fun <State: ParserState<State>, A, B> either(
        l: P<State, String, A>,
        r: P<State, String, B>
): P<State, String, Either<A, B>> = either({ s, _ -> s }, l, r)

/** Parse either `l` or `r`. If `l` consumes input and then fails, that failure
 *  will be reported. Otherwise, if `r` consumes input and then fails, that
 *  failure will be reported. Otherwise, both failures will be reported. */
infix fun <State: ParserState<State>, Value> P<State, String, Value>.or(
        r: P<State, String, Value>
): P<State, String, Value> = oneOf({ s, _ -> s }, this, r)

/** Parse using one of the parsers given. The first successful parse will be
 *  used. If a parse fails, and has consumed input, that failure will be
 *  reported. Otherwise, all failures will be reported. */
fun <State: ParserState<State>, Value> oneOf(
        parsers: Iterable<P<State, String, Value>>
): P<State, String, Value> = oneOf({ s, _ -> s }, parsers)

/** Parse using one of the parsers given. The first successful parse will be
 *  used. If a parse fails, and has consumed input, that failure will be
 *  reported. Otherwise, all failures will be reported. */
fun <State: ParserState<State>, Value> oneOf(
        vararg parsers: P<State, String, Value>
): P<State, String, Value> = oneOf(parsers.toList())

/** Greedily parse a list of `this` until an occurrence of `terminator` is
 *  parsed. Consumes `terminator`. */
fun <State: ParserState<State>, Error, Value> many(
        n: Int, p: P<State, Error, Value>
): P<State, Error, List<Value>> = when {
    n <= 0 -> many(p)
    else   -> p flatMap { x -> many(n - 1, p) map { xs -> listOf(x) + xs } }
}

/** Parse a value followed by EOF. */
fun <State: TokenParserState<State, Token>, Token, Value> thenEOF(
        p: P<State, String, Value>
): P<State, String, Value> = p { p keepLeft eof }

/** Parse a value until EOF. */
fun <State: TokenParserState<State, Token>, Token, Value> untilEOF(
        p: P<State, String, Value>
): P<State, String, List<Value>> = p { p until eof }

/** See `oneOf`. This is the vararg equivalent function. */
fun <State: ParserState<State>, Error, Value> oneOf(
        err: (String, State) -> Error,
        vararg parsers: P<State, Error, Value>
): P<State, Error, Value> = oneOf(err, parsers.toList())

/** Parse a value surrounded by `l` to the left and `r` to the right. */
fun <State: ParserState<State>, Error, Value> wrap(
        p: P<State, Error, Value>,
        l: P<State, Error, *>, r: P<State, Error, *> = l
): P<State, Error, Value> = l keepRight p keepLeft r

/** Parse a list of values separated by `sep`, surrounded by `l` to the left and
 *  `r` to the right. */
fun <State: ParserState<State>, Error, Value> wrapDelimited(
        p: P<State, Error, Value>,
        l: P<State, Error, *>, r: P<State, Error, *> = l,
        sep: P<State, Error, *>
): P<State, Error, List<Value>> = l keepRight (p sepBy sep) keepLeft r

/** Parse a list of values separated by `sep`, surrounded by `l` to the left and
 *  `r` to the right. If nothing is between `l` and `r`, yield an empty list. */
fun <State: ParserState<State>, Error, Value> wrapDelimited0(
        err: (String, State) -> Error,
        p: P<State, Error, Value>,
        l: P<State, Error, *>, r: P<State, Error, *> = l,
        sep: P<State, Error, *>
): P<State, Error, List<Value>> = l keepRight
        oneOf(err, r map { listOf<Value>() }, (p sepBy sep) keepLeft r)

/** Parse a list of values separated by `sep`, surrounded by `l` to the left and
 *  `r` to the right. If nothing is between `l` and `r`, yield an empty list. */
fun <State: ParserState<State>, Value> wrapDelimited0(
        p: P<State, String, Value>,
        l: P<State, String, *>, r: P<State, String, *> = l,
        sep: P<State, String, *>
): P<State, String, List<Value>> = wrapDelimited0({ s, _ -> s }, p, l, r, sep)

/** Convert the type of a parser, failing if the value it yields is not of that
 *  type. */
inline infix fun <State: ParserState<State>, Value, reified T: Any> P<State, String, Value>.
convertType(name: String): P<State, String, T> = this.satisfying { it is T }
        .map { it as T } .mapE { _ -> "expected $name" }

/** Convert the type of a parser, failing if the value it yields is not of that
 *  type. */
inline fun <State: ParserState<State>, Value, reified T: Any> convertType(
        p: P<State, String, Value>
): P<State, String, T> = p convertType ("type " + T::class.simpleName!!.toLowerCase())

/** Ensure the value yielded by a parser equals some value. */
infix fun <State: ParserState<State>, Value> P<State, String, Value>.equalTo(
        value: Value
): P<State, String, Value> = satisfying { it == value } mapE { _ -> "expected '$value'" }
