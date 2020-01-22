package astify.monadic.util

import astify.*
import astify.monadic.*
import astify.monadic.P
import astify.util.PositionedValue

fun tokenParser(
        keywords: Set<String> = setOf()
): P<TextStreamParserState, String, List<PositionedValue<Token>>> = p {
    // TODO: make this branching
    val token = positioned(oneOf(
            num,
            char map (::CharacterToken),
            string map (::StringToken),
            identifier map { if (keywords.contains(it)) KeywordToken(it) else IdentifierToken(it) },
            anything map { SymbolToken(it.toString()) }
    ))

    whitespace keepRight (token keepLeft whitespace until eof)
}

val num: P<TextStreamParserState, String, Token> = p {
    val fractionalPart = anything satisfying { it == '.' } keepRight integer
    (integer and optional(fractionalPart)) map { (i, f) ->
        if (f == null) IntegerToken(i)
        else NumberToken("$i.$f".toFloat())
    }
}
