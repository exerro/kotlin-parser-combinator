package astify.util

import astify.*
import astify.P

fun tokenParser(
        keywords: Set<String> = setOf()
): P<Char, PositionedValue<Token>> = P {
    val digit = satisfying { it.isDigit() } fmape { e, _ -> e.copy(error = "Digit expected") }
    val letter = satisfying { it.isLetter() } fmape { e, _ -> e.copy(error = "Letter expected") }
    val letterOrDigit = satisfying { it.isLetterOrDigit() } fmape { e, _ -> e.copy(error = "Letter or digit expected") }

    val integer = many(1, digit) map { String(it.toCharArray()).toInt() }
    val identifier = letter and many(letterOrDigit) map { (x, xs) -> x + String(xs.toCharArray()) }
    val float = (integer and ((equalTo('.')) keepRight integer))
            .map { (intPart, fractionalPart) -> "$intPart.$fractionalPart".toFloat() }
    val whitespace = many(satisfying { it.isWhitespace() })
    val charEscaped = ((equalTo('\\')) keepRight anything) map {
        when (it) {
            'n' -> '\n'
            else -> it
        }
    }
    val stringCharUnescaped = satisfying { it != '"' && it != '\\' }
    val charUnescaped = satisfying { it != '\'' && it != '\\' }
    val stringChar = charEscaped or stringCharUnescaped
    val char = wrap(charEscaped or charUnescaped, equalTo('\''))
    val string = wrap(many(stringChar), equalTo('"')) map { String(it.toCharArray()) }
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
