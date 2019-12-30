package astify.util

import astify.*
import astify.P

fun tokenParser(
        keywords: Set<String> = setOf()
): CP<PositionedValue<Token>> = charP {
    // TODO: make this branching
    val token = whitespace keepRight oneOf(
            float map { NumberToken(it) },
            integer map (::IntegerToken),
            char map (::CharacterToken),
            string map (::StringToken),
            identifier map { if (keywords.contains(it)) KeywordToken(it) else IdentifierToken(it) },
            anything map { SymbolToken(it.toString()) }
    )

    positioned(token)
}

fun lexerParser(keywords: Set<String> = setOf()): P<Char, ParserState<Token>> = P.new { s ->
    P<Char, ParserState<Token>> {
        many(tokenParser(keywords)) map {
            ListParserState.new(it, s.stream)
        }
    }.parse(s)
}
