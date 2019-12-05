package astify

class TextStream private constructor(
        val raw: String,
        private val chars: CharArray,
        private val index: Int
) {
    constructor(str: String): this(str, str.toCharArray(), 0)

    val char: Char = chars[index]
    val next by lazy {
        if (index + 1 < chars.size) TextStream(raw, chars, index + 1) else null
    }
}
