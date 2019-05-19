
/** A token from some text stream */
data class Token(val type: String, val text: String, private val pos: Position): Positioned<String>() {
    override fun getValue(): String = text
    override fun getPosition(): Position = pos

    override fun toString(): String {
        return "'$text' @ ${getPosition().getPositionString()}"
    }
}

typealias TokenType = String

// integer constants for common token types
const val TOKEN_EOF = "EOF"
const val TOKEN_INT = "integer"
const val TOKEN_FLOAT = "float"
const val TOKEN_STR = "string"
const val TOKEN_CHAR = "character"
const val TOKEN_SYM = "symbol"
const val TOKEN_IDENT = "identifier"
const val TOKEN_KEYWORD = "keyword"
