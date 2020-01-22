package astify.monadic.util

import astify.Position
import astify.TextStream
import astify.monadic.PositionedTokenParserState

class TextStreamParserState(
        stream: TextStream,
        override val position: Position,
        val lastPosition: Position
): PositionedTokenParserState<TextStreamParserState, Char, Position> {
    override val token= stream.char
    override val next by lazy {
        TextStreamParserState(stream.next, position.after(1), position)
    }

    companion object {
        fun new(stream: TextStream)
                = TextStreamParserState(stream, Position(0), Position.start)
    }
}
