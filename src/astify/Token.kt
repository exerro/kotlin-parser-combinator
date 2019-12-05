package astify

sealed class Token
data class IntegerToken(val value: Int): Token()
data class NumberToken(val value: Float): Token()
data class StringToken(val value: String): Token()
data class CharacterToken(val value: Char): Token()
data class IdentifierToken(val value: String): Token()
data class KeywordToken(val value: String): Token()
data class SymbolToken(val value: Char): Token()
