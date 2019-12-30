package astify.util

import astify.ParserState
import astify.Position
import astify.TextStream

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
