package astify.monadic

import astify.Position
import astify.TextStream

class ParseError(
        override val message: String
): Throwable() {
    companion object {
        fun <State: PositionedTokenParserState<State, *, Position>> create(
                fail: ParseResult.Failure<State, String>,
                stream: TextStream
        ) = ParseError(formatErrorInternal(fail, stream, true))
    }
}

private fun <State: PositionedTokenParserState<State, *, Position>> formatErrorInternal(
        fail: ParseResult.Failure<State, String>,
        str: TextStream,
        includePosition: Boolean
): String {
    val tk = fail.state.token
    val pos = fail.state.position
    val start = fail.error + if (includePosition) "\n" + pos.linePointer(str) else ""

    return if (fail.causes.isEmpty()) start else {
        val includeSubPosition = fail.causes.any { it.state.position != pos }
        start + ("\n caused by\n" + fail.causes.joinToString("\n") {
            "\t" + formatErrorInternal(it, str, includeSubPosition)
                    .replace("\n", "\n\t")
        })
    }
}
