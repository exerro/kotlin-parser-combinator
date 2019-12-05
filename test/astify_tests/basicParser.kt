package astify_tests

import assertValueEquals
import astify.*
import astify.util.lexerParser
import test
import writeln

fun basicParsers() = test("ASTify Basic Parsers") {
    val str = TextStream("abcdef")
    val state = TextStreamParserState(str, Position.start)

    assertValueEquals(parse(str, state, parser2<Char, Char> { any }), 'a')
    assertValueEquals(parse(str, state, parser2<Char, Char> { any preceededBy any }), 'b')

    val results = parse("hello world 3217 3.2 \"str\" 'c' \"\\\"\" '\\''", lexerParser(setOf("hello")), parser2<Token, List<Token>> { many(any) })

    if (assertValueEquals(results.size, 8)) {
        assertValueEquals(results[0], KeywordToken("hello"))
        assertValueEquals(results[1], IdentifierToken("world"))
        assertValueEquals(results[2], IntegerToken(3217))
        assertValueEquals(results[3], NumberToken(3.2f))
        assertValueEquals(results[4], StringToken("str"))
        assertValueEquals(results[5], CharacterToken('c'))
        assertValueEquals(results[6], StringToken("\""))
        assertValueEquals(results[7], CharacterToken('\''))
    }
    else {
        writeln(results.joinToString("\n"))
    }
}
