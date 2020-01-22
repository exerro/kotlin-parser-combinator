package astify.monadic

sealed class ParseResult<out State: ParserState<State>, out Error, out Value> {
    data class Success<out State: ParserState<State>, out Value>(
            val value: Value, val state: State
    ): ParseResult<State, Nothing, Value>()

    data class Failure<out State: ParserState<State>, out Error>(
            val error: Error, val state: State,
            val causes: List<Failure<State, Error>> = listOf()
    ): ParseResult<State, Error, Nothing>()
}

fun <State: ParserState<State>, Value>
ParseResult.Success<State, *>.withValue(value: Value)
        = ParseResult.Success(value, state)

fun <State: ParserState<State>, Error>
ParseResult.Failure<State, Error>.withError(error: Error)
        = ParseResult.Failure(error, state, causes)
