package astify.monadic

interface ParserState<out This> {
    val next: This
}

interface TokenParserState<out This, out Token>: ParserState<This> {
    val token: Token?
}

interface PositionedTokenParserState<out This, out Token, out Position>
    : TokenParserState<This, Token> {
    val position: Position
}
