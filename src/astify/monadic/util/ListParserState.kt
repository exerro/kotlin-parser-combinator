package astify.monadic.util

import astify.Position
import astify.TextStream
import astify.monadic.PositionedTokenParserState
import astify.util.PositionedValue

class ListParserState<T>(
        private val list: List<PositionedValue<T>>,
        private val index: Int,
        override val position: Position,
        val lastPosition: Position,
        stream: TextStream
): PositionedTokenParserState<ListParserState<T>, T, Position> {
    override val token: T? = list.getOrNull(index)?.let { (v, p) -> v }
    override val next by lazy {
        ListParserState(list, index + 1, position.after(1), position, stream)
    }

    companion object {
        fun <T> new(list: List<PositionedValue<T>>, stream: TextStream): ListParserState<T>
                = ListParserState(list, 0,
                list.getOrNull(0)?.position ?: Position.start,
                Position.start, stream)
    }
}
