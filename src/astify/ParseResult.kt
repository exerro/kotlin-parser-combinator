package astify

sealed class ParseResult<out Token, out T>

class ParseSuccess<out Token, out T>(
        val value: T,
        val position: Position,
        val nextState: ParserState<Token>
): ParseResult<Token, T>()

class ParseFail(
        val error: String,
        val position: Position,
        val causes: List<ParseFail> = listOf()
): ParseResult<Nothing, Nothing>() {
    fun formatError(str: TextStream): String {
        return error + "\n" + position.linePointer(str) + "\n caused by" + causes
                .map { it.formatError(str) }
                .joinToString("\n") {
                    it.replace("\n", "\n\t")
                }
    }
}

class ParseError(
        val fail: ParseFail,
        override val message: String
): Throwable() {
    constructor(fail: ParseFail, stream: TextStream): this(fail, fail.formatError(stream))
}
