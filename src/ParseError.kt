
data class ParseError(val error: String, val position: Position, val causes: Set<ParseError>? = null) : Throwable() {
    fun getString(getSource: () -> TextStream): String {
        return position.getErrorString(position.getPositionString() + ": " + error, getSource()) + if (causes == null || causes.isEmpty()) "" else "\n  ... caused by:\n" + causes.joinToString("\n") {cause ->
            "\t" + cause.getShortString(getSource).replace("\n", "\n\t")
        }
    }

    private fun getShortString(getSource: () -> TextStream): String {
        return position.getErrorString(error, getSource()) + if (causes == null || causes.isEmpty()) "" else "\n  ... caused by:\n" + causes.joinToString("\n") {cause ->
            "\t" + cause.getString(getSource).replace("\n", "\n\t")
        }
    }
}
