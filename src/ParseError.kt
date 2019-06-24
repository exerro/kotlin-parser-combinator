
/** Holds an error message and position, and a list of causes if applicable */
data class ParseError(val error: String, val position: Position, val causes: Set<ParseError> = setOf()) : Throwable() {
    /** Return a string representation of the error, showing source lines and positions */
    fun getString(source: TextStream): String {
        return position.getErrorString(position.getPositionString() + ": " + error, source) + if (causes.isEmpty()) "" else "\n  ... caused by:\n" + causes.joinToString("\n") { cause ->
            "\t" + cause.getShortString(source).replace("\n", "\n\t")
        }
    }

    private fun getShortString(source: TextStream): String {
        return position.getErrorString(error, source) + if (causes.isEmpty()) "" else "\n  ... caused by:\n" + causes.joinToString("\n") { cause ->
            "\t" + cause.getString(source).replace("\n", "\n\t")
        }
    }
}
