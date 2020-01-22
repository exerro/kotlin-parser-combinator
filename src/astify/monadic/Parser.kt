package astify.monadic

open class P<State: ParserState<State>, out Error, out Value>(
        val parse: (State) -> ParseResult<State, Error, Value>
) {
    companion object {
        fun <State: ParserState<State>, Value> pure(
                value: Value
        ): P<State, Nothing, Value> = P { s -> ParseResult.Success(value, s) }
    }
}

infix fun <State: ParserState<State>, Error, Value, R> P<State, Error, Value>.map(
        fn: (Value) -> R
): P<State, Error, R> = P { s -> when (val v = parse(s)) {
    is ParseResult.Success -> v.withValue(fn(v.value))
    is ParseResult.Failure -> v
} }

infix fun <State: ParserState<State>, Error, Value> P<State, Error, Value>.mapE(
        fn: (Error, State, List<ParseResult.Failure<State, Error>>) -> Error
): P<State, Error, Value> = P { s -> when (val v = parse(s)) {
    is ParseResult.Success -> v
    is ParseResult.Failure -> v.withError(fn(v.error, v.state, v.causes))
} }

infix fun <State: ParserState<State>, Error, Value> P<State, Error, Value>.mapE(
        fn: (Error) -> Error
): P<State, Error, Value> = P { s -> when (val v = parse(s)) {
    is ParseResult.Success -> v
    is ParseResult.Failure -> v.withError(fn(v.error))
} }

infix fun <State: ParserState<State>, Error, Value, R> P<State, Error, Value>.flatMap(
        fn: (Value, State) -> P<State, Error, R>
): P<State, Error, R> = P { s -> when (val v = parse(s)) {
    is ParseResult.Success -> fn(v.value, v.state).parse(v.state)
    is ParseResult.Failure -> v
} }

infix fun <State: ParserState<State>, Error, Value, R> P<State, Error, Value>.flatMap(
        fn: (Value) -> P<State, Error, R>
): P<State, Error, R> = P { s -> when (val v = parse(s)) {
    is ParseResult.Success -> fn(v.value).parse(v.state)
    is ParseResult.Failure -> v
} }

infix fun <State: ParserState<State>, Error, Value> P<State, Error, Value>.flatMapE(
        fn: (Error, State, List<ParseResult.Failure<State, Error>>) -> P<State, Error, Value>
): P<State, Error, Value> = P { s -> when (val v = parse(s)) {
    is ParseResult.Success -> v
    is ParseResult.Failure -> fn(v.error, v.state, v.causes).parse(v.state)
} }

infix fun <State: ParserState<State>, Error, Value> P<State, Error, Value>.flatMapE(
        fn: (Error) -> P<State, Error, Value>
): P<State, Error, Value> = P { s -> when (val v = parse(s)) {
    is ParseResult.Success -> v
    is ParseResult.Failure -> fn(v.error).parse(v.state)
} }
