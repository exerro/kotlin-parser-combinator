package astify.monadic.util

import astify.*
import astify.monadic.*
import astify.monadic.P
import astify.util.PositionedValue
import astify.util.positioned

val <State: TokenParserState<State, Token>> Parsing<State, String>.
        identifier: P<State, String, IdentifierToken>
    get() = anything convertType "identifier"

val <State: TokenParserState<State, Token>> Parsing<State, String>.
        integer: P<State, String, IntegerToken>
    get() = anything convertType "integer"

val <State: TokenParserState<State, Token>> Parsing<State, String>.
        number: P<State, String, NumberToken>
    get() = anything convertType "number"

val <State: TokenParserState<State, Token>> Parsing<State, String>.
        symbol: P<State, String, SymbolToken>
    get() = anything convertType "symbol"

val <State: TokenParserState<State, Token>> Parsing<State, String>.
        string: P<State, String, StringToken>
    get() = anything convertType "string"

val <State: TokenParserState<State, Token>> Parsing<State, String>.
        character: P<State, String, CharacterToken>
    get() = anything convertType "character"

val <State: TokenParserState<State, Token>> Parsing<State, String>.
        keyword: P<State, String, KeywordToken>
    get() = anything convertType "keyword"

// Utility /////////////////////////////////////////////////////////////////////

val <State: TokenParserState<State, Token>> Parsing<State, String>.
        identifierValue get() = identifier map { it.value }

val <State: TokenParserState<State, Token>> Parsing<State, String>.
        integerValue get() = integer map { it.value }

val <State: TokenParserState<State, Token>> Parsing<State, String>.
        numberValue get() = number map { it.value }

val <State: TokenParserState<State, Token>> Parsing<State, String>.
        symbolValue get() = symbol map { it.value }

val <State: TokenParserState<State, Token>> Parsing<State, String>.
        stringValue get() = string map { it.value }

val <State: TokenParserState<State, Token>> Parsing<State, String>.
        characterValue get() = character map { it.value }

val <State: TokenParserState<State, Token>> Parsing<State, String>.
        keywordValue get() = keyword map { it.value }

// Operators ///////////////////////////////////////////////////////////////////

fun <State: TokenParserState<State, Token>> Parsing<State, String>.
        keyword(kw: String): P<State, String, KeywordToken>
        = keyword.satisfying { it.value == kw }
        . mapE { _ -> "expected keyword '$kw'" }

fun <State: PositionedTokenParserState<State, Token, Position>> Parsing<State, String>.
        symbol(s: String): P<State, String, SymbolToken> {
    val symbols = s
            .toCharArray()
            .map(Char::toString)
            .map { ch -> anythingPositioned
                    .satisfying { (tk) -> tk is SymbolToken && tk.value == ch }
                    .map { it.second } }

    return symbols.drop(1).fold(symbols[0]) { pre, next ->
        (pre and next) satisfying { (a, b) -> b follows a } map { (a, b) -> a to b }
    }
            .map { SymbolToken(s) }
            .mapE { _ -> "expected symbol '$s'" }
}

fun <State: PositionedTokenParserState<State, Token, Position>, Value> wrapSymbols(
        p: P<State, String, Value>,
        open: String = "(",
        close: String = ")"
): P<State, String, Value>
        = p { wrap(p, symbol(open), symbol(close)) }

fun <State: PositionedTokenParserState<State, Token, Position>, Value> wrapDelimitedSymbols(
        p: P<State, String, Value>,
        open: String = "(",
        close: String = ")",
        sep: String = ","
): P<State, String, List<Value>>
        = p { wrapDelimited(p, symbol(open), symbol(close), symbol(sep)) }

fun <State: PositionedTokenParserState<State, Token, Position>, Value> wrapDelimited0Symbols(
        p: P<State, String, Value>,
        open: String = "(",
        close: String = ")",
        sep: String = ","
): P<State, String, List<Value>>
        = p { wrapDelimited0(p, symbol(open), symbol(close), symbol(sep)) }
