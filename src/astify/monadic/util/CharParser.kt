package astify.monadic.util

import astify.monadic.*

val <State: TokenParserState<State, Char>> Parsing<State, String>.
        digit: P<State, String, Char>
    get() = anything satisfying { it.isDigit() } mapE { _ -> "expected digit" }

val <State: TokenParserState<State, Char>> Parsing<State, String>.
        letter: P<State, String, Char>
    get() = anything satisfying { it.isLetter() } mapE { _ -> "expected letter" }

val <State: TokenParserState<State, Char>> Parsing<State, String>.
        letterOrDigit: P<State, String, Char>
    get() = anything satisfying { it.isLetterOrDigit() } mapE { _ -> "expected letter or digit" }

val <State: TokenParserState<State, Char>> Parsing<State, String>.
        whitespace: P<State, String, List<Char>>
    get() = many(anything satisfying { it.isWhitespace() })

val <State: TokenParserState<State, Char>> Parsing<State, String>.
        integer: P<State, String, Int>
    get() = many(1, digit) map { String(it.toCharArray()).toInt() }

val <State: TokenParserState<State, Char>> Parsing<State, String>.
        identifier: P<State, String, String>
    get() = (letter and many(letterOrDigit))
            .map { (x, xs) -> x + String(xs.toCharArray()) }

val <State: TokenParserState<State, Char>> Parsing<State, String>.
        float: P<State, String, Float>
    get() = (integer and ((anything equalTo '.') keepRight integer))
        .map { (intPart, fractionalPart) -> "$intPart.$fractionalPart".toFloat() }

val <State: TokenParserState<State, Char>> Parsing<State, String>.
        charEscaped: P<State, String, Char>
    get() = ((anything equalTo '\\') keepRight anything) map escapeCharacter

val <State: TokenParserState<State, Char>> Parsing<State, String>.
        stringCharUnescaped: P<State, String, Char>
    get() = anything satisfying { it != '"' && it != '\\' }

val <State: TokenParserState<State, Char>> Parsing<State, String>.
        charUnescaped: P<State, String, Char>
    get() = anything satisfying  { it != '\'' && it != '\\' }

val <State: TokenParserState<State, Char>> Parsing<State, String>.
        stringChar: P<State, String, Char>
    get() = charEscaped or stringCharUnescaped

val <State: TokenParserState<State, Char>> Parsing<State, String>.
        char: P<State, String, Char>
    get() = wrap(charEscaped or charUnescaped, anything equalTo '\'')

val <State: TokenParserState<State, Char>> Parsing<State, String>.
        string: P<State, String, String>
    get() = wrap(many(stringChar), anything equalTo '"') map { String(it.toCharArray()) }

val escapeCharacter: (Char) -> Char = { c -> when (c) {
    'n' -> '\n'
    else -> c
} }
