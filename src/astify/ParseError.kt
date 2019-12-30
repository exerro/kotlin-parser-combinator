package astify

class ParseError(
        val fail: ParseFail,
        override val message: String
): Throwable() {
    constructor(fail: ParseFail, stream: TextStream): this(fail, fail.formatError(stream))
}
