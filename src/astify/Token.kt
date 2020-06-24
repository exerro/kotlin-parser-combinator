package astify

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

sealed class Token
data class IntegerToken(val value: Int): Token()
data class NumberToken(val value: Float): Token()
data class StringToken(val value: String): Token()
data class CharacterToken(val value: Char): Token()
data class IdentifierToken(val value: String): Token()
data class KeywordToken(val value: String): Token()
data class SymbolToken(val value: String): Token()
data class CommentToken(val value: String): Token()

val Any?.tokenTypeName get() = when (this) {
    null -> "null"
    is Token -> this::class.simpleName!!.replace("Token", "").toLowerCase()
    else -> this::class.simpleName ?: "null"
}

inline fun <reified T: Any> KClass<T>.tokenTypeName() = when {
    isSubclassOf(Token::class) -> simpleName!!.replace("Token", "").toLowerCase()
    else -> simpleName ?: "null"
}
