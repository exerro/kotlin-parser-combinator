package astify

abstract class ParserState<out Token>(
        val position: Position,
        val lastPosition: Position,
        val stream: TextStream
) {
    abstract val token: Token?
    abstract val nextState: ParserState<Token>
}
