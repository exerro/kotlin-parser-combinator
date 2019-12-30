package astify

import astify.util.PositionedValue

abstract class ParserState<out Token>(
        val position: Position,
        val lastPosition: Position,
        val stream: TextStream
) {
    abstract val token: Token?
    abstract val nextState: ParserState<Token>
}

class TextStreamParserState(
        stream: TextStream,
        position: Position,
        lastPosition: Position
): ParserState<Char>(position, lastPosition, stream) {
    override val token = stream.char
    override val nextState by lazy {
        TextStreamParserState(stream.next, position.after(1), position)
    }

    companion object {
        fun new(stream: TextStream)
                = TextStreamParserState(stream, Position(0), Position.start)
    }
}

class ListParserState<T>(
        private val list: List<PositionedValue<T>>,
        private val index: Int,
        position: Position,
        lastPosition: Position,
        stream: TextStream
): ParserState<T>(list.getOrNull(index) ?.position ?: position, lastPosition, stream) {
    override val token = list.getOrNull(index) ?.value
    override val nextState by lazy {
        ListParserState(list, index + 1, this.position.after(1), this.position, stream)
    }

    companion object {
        fun <T> new(list: List<PositionedValue<T>>, stream: TextStream): ListParserState<T>
                = ListParserState(list, 0,
                list.getOrNull(0) ?.position ?: Position.start,
                Position.start, stream)
    }
}
