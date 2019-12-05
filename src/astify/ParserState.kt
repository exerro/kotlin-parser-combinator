package astify

import astify.util.ValuePositionPair

abstract class ParserState<out Token>(
        val position: Position
) {
    abstract val token: Token?
    abstract val next: ParserState<Token>
}

class TextStreamParserState(
        private val stream: TextStream?,
        position: Position
): ParserState<Char>(position) {
    override val token = stream?.char
    override val next by lazy {
        TextStreamParserState(stream?.next, position.after(1))
    }
}

class ListParserState<T>(
        private val list: List<ValuePositionPair<T>>,
        private val index: Int,
        position: Position
): ParserState<T>(list.getOrNull(index) ?.position ?: position) {
    override val token = list.getOrNull(index) ?.value
    override val next by lazy {
        ListParserState(list, index + 1, position)
    }
}
