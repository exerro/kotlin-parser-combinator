package astify

class TextStream private constructor(
        val raw: String,
        private val chars: CharArray,
        private val index: Int
) {
    val char: Char? = chars.getOrNull(index)
    val next by lazy { TextStream(raw, chars, index + 1) }

    companion object {
        fun create(str: String) = TextStream(str, str.toCharArray(), 0)
    }
}
