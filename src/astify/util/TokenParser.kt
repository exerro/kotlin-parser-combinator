package astify.util

import astify.*

fun <T> tokenP(fn: TokenParser.() -> P<Token, T>) = fn(TokenParser())

typealias TP<T> = P<Token, T>

open class TokenParser: Parser<Token>() {
    val identifier: TP<IdentifierToken> = convertType("identifier")
    val integer: TP<IntegerToken> = convertType("integer")
    val number: TP<NumberToken> = convertType("number")
    val symbol: TP<SymbolToken> = convertType("symbol")
    val string: TP<StringToken> = convertType("string")
    val character: TP<CharacterToken> = convertType("character")
    val keyword: TP<KeywordToken> = convertType("keyword")

    // Operators ///////////////////////////////////////////////////////////////

    fun keyword(keyword: String): TP<KeywordToken>
            = positioned(this.keyword) satisfying { it.value == keyword } fmape { f, _ ->
                f.copy(error = "Expected keyword '$keyword'") } map { it.value }

    fun symbol(s: String): TP<SymbolToken> {
        val symbols = s
                .toCharArray()
                .map(Char::toString)
                .map { ch -> positioned(convertType<SymbolToken>())
                        .satisfying { it.value == ch }
                        .map { it } }

        return symbols.drop(1).fold(symbols[0]) { pre, next ->
            pre bind { a -> next fmap { (b, n) ->
                if (b.position follows a.position) {
                    val tok = SymbolToken(a.value.value + b.value.value)
                    val pos = a.position to b.position
                    ParseSuccess(tok positioned pos, n)
                }
                else
                    ParseFail("", b.position)
            } }
        } map { it.value } fmape { e, _ -> e.copy(error = "Expected symbol '$symbol'") }
    }

    fun <T> parens(p: TP<T>): TP<T> = wrap(p, symbol("("), symbol(")"))
}
