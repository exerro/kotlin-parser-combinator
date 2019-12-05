package astify.util

import astify.*

class SequenceParserContext<Token>(
        private var state: ParserState<Token>
) {
    fun <T> parse(p: P<Token, T>) = when (val r = p(state)) {
        is ParseSuccess -> {
            state = r.nextState
            r.value to r.position
        }
        is ParseFail -> throw SequenceParserException(r)
    }

    fun <T> parsev(p: P<Token, T>) = parse(p).first

    fun <T> apply(fn: SequenceParserContext<Token>.() -> ValuePositionPair<T>) = try {
        val result = fn(this)
        ParseSuccess(result.value, result.position, state)
    } catch (e: SequenceParserException) { e.err }
}

private class SequenceParserException(val err: ParseFail): Throwable()
