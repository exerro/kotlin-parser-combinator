package astify

fun <Token, T> parse(
        str: TextStream,
        state: ParserState<Token>,
        parser: P<Token, T>
): T = when (val r = parser.parse(state)) {
    is ParseSuccess -> r.value
    is ParseFail -> throw ParseError(r, str)
}

fun <Token, T> parse(str: TextStream, lexer: P<Char, ParserState<Token>>, parser: P<Token, T>)
        = parse(str, parse(str, TextStreamParserState.new(str), lexer), parser)

fun <Token, T> parse(str: String, lexer: P<Char, ParserState<Token>>, parser: P<Token, T>)
        = parse(TextStream.create(str), lexer, parser)
