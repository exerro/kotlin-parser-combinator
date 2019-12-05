package astify

import astify.util.positioned

fun <Token, T> parse(
        str: TextStream,
        state: ParserState<Token>,
        parser: P<Token, T>
): T = when (val r = parser(state)) {
    is ParseSuccess -> r.value
    is ParseFail -> throw ParseError(r, str)
}

fun <Token, T> parse(str: TextStream, lexer: P<Char, ParserState<Token>>, parser: P<Token, T>)
        = parse(str, parse(str, TextStreamParserState(str, Position.start), lexer), parser)

fun <Token, T> parse(str: String, lexer: P<Char, ParserState<Token>>, parser: P<Token, T>)
        = parse(TextStream(str), lexer, parser)

//////////////////////////////////////////////////////////////////////////////////////////

fun main() {
//    println(parse("361.2 321 82", charLexer, lexerParser).assertSuccess())
}
