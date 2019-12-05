package astify.util

import astify.*

val ParserContext<Token>.identifier get()
= any.asType<Token, IdentifierToken> { "Expected identifier, got ${it.tokenTypeName}" }

val ParserContext<Token>.integer get()
= any.asType<Token, IntegerToken> { "Expected integer, got ${it.tokenTypeName}" }

val ParserContext<Token>.number get()
= any.asType<Token, NumberToken> { "Expected number, got ${it.tokenTypeName}" }

val ParserContext<Token>.symbol get()
= any.asType<Token, SymbolToken> { "Expected symbol, got ${it.tokenTypeName}" }

val ParserContext<Token>.string get()
= any.asType<Token, StringToken> { "Expected string, got ${it.tokenTypeName}" }

val ParserContext<Token>.character get()
= any.asType<Token, CharacterToken> { "Expected character, got ${it.tokenTypeName}" }

fun ParserContext<Token>.keyword(keyword: String): P<Token, KeywordToken>
        = any.asType<KeywordToken>().satisfying("Expected keyword '$keyword'") { it.value == keyword }

fun ParserContext<Token>.symbol(symbol: String): P<Token, SymbolToken> {
    val symbols = symbol
            .toCharArray()
            .map(Char::toString)
            .map { ch -> any.asType<SymbolToken>() satisfying { it.value == ch } }

    return symbols.drop(1).fold(symbols[0]) { pre, next ->
        pre bind { a, ap -> next bind { b, bp ->
            inject(SymbolToken(a.value + b.value), ap to bp).satisfying { bp follows ap }
        } }
    } .fmaperr { ParseFail("Expected symbol '$symbol'", it.position) }
}
