package astify.util

import astify.*

fun tokenParser(
        keywords: Set<String> = setOf()
): P<Char, ValuePositionPair<Token>> = parser2 {
    val digit = any.satisfying("Digit expected") { it.isDigit() }
    val letter = any.satisfying("Letter expected") { it.isLetter() }
    val letterOrDigit = any.satisfying("Letter or digit expected") { it.isLetterOrDigit() }

    val integer = many(1, digit) mapv { String(it.toCharArray()).toInt() }
    val identifier = letter then many(letterOrDigit) mapv { (x, xs) -> x + String(xs.toCharArray()) }
    val float = (integer then (integer preceededBy (any equalTo '.')))
            .mapv { (intPart, fractionalPart) -> "$intPart.$fractionalPart".toFloat() }
    val whitespace = many(any.satisfying { it.isWhitespace() })
    val charEscaped = (any preceededBy (any equalTo '\\')) mapv { when (it) {
        'n' -> '\n'
        else -> it
    } }
    val stringCharUnescaped = any satisfying { it != '"' && it != '\\' }
    val charUnescaped = any satisfying { it != '\'' && it != '\\' }
    val stringChar = charEscaped or stringCharUnescaped
    val char = wrap(charEscaped or charUnescaped, any equalTo '\'')
    val string = wrap(many(stringChar), any equalTo '"') mapv { String(it.toCharArray()) }
    val token = oneOf(
            float mapv { NumberToken(it) },
            integer mapv (::IntegerToken),
            char mapv (::CharacterToken),
            string mapv (::StringToken),
            identifier mapv { if (keywords.contains(it)) KeywordToken(it) else IdentifierToken(it) },
            any mapv { SymbolToken(it.toString()) }
    ) preceededBy whitespace

    token map { x, p -> x positioned p positioned p }
}

fun lexerParser(keywords: Set<String> = setOf())
        = parser2<Char, ParserState<Token>> { many(tokenParser(keywords)) mapv {
    ListParserState(it, 0, it.getOrNull(0) ?.position ?: Position.start)
} }
