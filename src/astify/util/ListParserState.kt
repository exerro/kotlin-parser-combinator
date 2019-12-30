package astify.util

import astify.ParserState
import astify.Position
import astify.TextStream

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
                list.getOrNull(0)?.position ?: Position.start,
                Position.start, stream)
    }
}
