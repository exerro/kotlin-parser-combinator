package astify

sealed class ParseResult<out Token, out T> {
    abstract fun <R> fmap(fn: (T) -> R): ParseResult<Token, R>
}

data class ParseSuccess<out Token, out T>(
        val value: T,
        val nextState: ParserState<Token>
): ParseResult<Token, T>() {
    override fun <R> fmap(fn: (T) -> R)
            = ParseSuccess(fn(value), nextState)
}

data class ParseFail(
        val error: String,
        val position: Position,
        val causes: List<ParseFail> = listOf()
): ParseResult<Nothing, Nothing>() {
    fun formatError(str: TextStream): String {
        return error + "\n" + position.linePointer(str) + if (causes.isNotEmpty()) ("\n caused by\n" + causes.joinToString("\n") { "\t" + it.formatError(str).replace("\n", "\n\t") }) else ""
    }

    override fun <R> fmap(fn: (Nothing) -> R) = this
}

class ParseError(
        val fail: ParseFail,
        override val message: String
): Throwable() {
    constructor(fail: ParseFail, stream: TextStream): this(fail, fail.formatError(stream))
}
