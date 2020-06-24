package astify.monadic.util

import astify.*
import astify.monadic.*
import astify.monadic.P
import astify.util.PositionedValue

typealias TokenP = P<TextStreamParserState, String, PositionedValue<Token>>
typealias LexerP = P<TextStreamParserState, String, List<PositionedValue<Token>>>

fun tokenParser(keywords: Set<String> = setOf()): TokenP = p {
    // TODO: make this branching
    val token = positioned(oneOf(
            numericToken,
            char map ::CharacterToken,
            string map ::StringToken,
            identifier map { if (keywords.contains(it)) KeywordToken(it) else IdentifierToken(it) },
            anything map { SymbolToken(it.toString()) }
    ))

    token
}

fun lineComment(s: String): TokenP = p {
    val line = anything until (anything equalTo '\n' or eof) map { it.joinToString("") }
    positioned(sym(s) keepRight line map(::CommentToken))
}

fun multiLineComment(open: String, close: String): TokenP = p {
    val comment = anything until (sym(close) or eof) map { it.joinToString("") }
    positioned(sym(open) keepRight comment map(::CommentToken))
}

fun lexer(token: P<TextStreamParserState, String, PositionedValue<Token>>)
        : P<TextStreamParserState, String, List<PositionedValue<Token>>>
        = p { whitespace keepRight (token keepLeft whitespace until eof) }

private fun sym(s: String): P<TextStreamParserState, String, Unit> = p {
    s.drop(1).fold(anything equalTo s[0]) { acc, c ->
        acc keepRight (anything equalTo c)
    } map { Unit }
}

val numericToken: P<TextStreamParserState, String, Token> = p {
    val fractionalPart = anything satisfying { it == '.' } keepRight integer
    (integer and optional(fractionalPart)) map { (i, f) ->
        if (f == null) IntegerToken(i)
        else NumberToken("$i.$f".toFloat())
    }
}
