
/** A token from some text stream */
class Token(val type: Int, val text: String, private val pos: Position): Positioned<String>() {
    override fun getValue(): String = text
    override fun getPosition(): Position = pos

    override fun toString(): String {
        return "'$text' @ ${getPosition().getPositionString()}"
    }
}
