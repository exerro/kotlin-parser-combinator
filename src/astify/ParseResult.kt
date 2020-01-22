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
    fun formatError(str: TextStream)
            = formatErrorInternal(this, str, true)

    override fun <R> fmap(fn: (Nothing) -> R) = this
}

private fun formatErrorInternal(fail: ParseFail, str: TextStream, includePosition: Boolean): String {
    val start = fail.error + if (includePosition) "\n" + fail.position.linePointer(str) else ""

    return if (fail.causes.isEmpty()) start else {
        val includeSubPosition = !fail.causes.all { it.position == fail.position }
        start + ("\n caused by\n" + fail.causes.joinToString("\n") {
            "\t" + formatErrorInternal(it, str, includeSubPosition)
                    .replace("\n", "\n\t")
        })
    }
}
