import java.lang.StringBuilder

/** Represents a position in some source of text */
data class Position(val line1: Int, val char1: Int, val line2: Int = line1, val char2: Int = char1) {
    /** Returns a string representation of this position */
    fun getPositionString(): String {
        return if (line1 == line2) {
            if (char1 == char2) "[line $line1 col $char1]"
            else "[line $line1 col $char1 .. $char2]"
        }
        else "[line $line1 col $char1 .. line $line2 col $char2]"
    }

    /** Returns a string containing an error message and an indicator of where in its source the error targets
     *
     *  Note: the text stream will be reset during this method */
    fun getErrorString(error: String, source: TextStream): String {
        source.reset()
        return if (line1 == line2) {
            "$error\n" +
                    "    ${this.getSourceLines(source).first}\n" +
                    "    ${rep(" ", char1 - 1)}${rep("^", char2 - char1 + 1)}"
        }
        else {
            val (l1, l2) = this.getSourceLines(source)
            "$error\n" +
                    "    $l1\n" +
                    "    ${rep(" ", char1 - 1)}${rep("^", l1.length - char1 + 1)}...\n" +
                    "    $l2\n" +
                    "... ${rep("^", char2)}"
        }
    }

    override fun toString(): String
            = getPositionString()
}
/** Returns a position spanning `this` and `other` */
infix fun Position.to(other: Position): Position = Position(line1, char1, other.line2, other.char2)

/** Returns a position spanning `this` and a position `length` chars past the start of this position */
infix fun Position.extendTo(length: Int): Position = Position(line1, char1, line1, char1 + length - 1)

/** Returns a position 1 character after the end of `this` */
infix fun Position.after(chars: Int): Position = Position(line2, char2 + chars)

/** Returns true if this position is immediately after the other position */
infix fun Position.follows(other: Position): Boolean = line1 == other.line2 && char1 == other.char2 + 1

/** Returns the last position of a list of positions, compared by their first position */
fun List<Position>.last(): Position
        = drop(1).fold(this[0]) { a, b ->
    if (a.line1 > b.line1) a else if (a.line1 == b.line1 && a.char1 > b.char1) a else b
}

private fun Position.getSourceLines(stream: TextStream): Pair<String, String> {
    return if (line1 == line2) {
        val line = getLine(stream, line1)
        Pair(line, line)
    }
    else {
        val l1 = getLine(stream, line1)
        val l2 = getLine(stream, line2 - line1)
        Pair(l1, l2)
    }
}

private fun rep(str: String, count: Int): String = str.repeat(count)

private fun getLine(stream: TextStream, line: Int): String {
    var ln = line
    val result = StringBuilder()

    while (ln > 1) {
        if (!stream.isEOF()) { if (stream.readNextChar() == '\n') { ln-- } }
        else { return "" }
    }

    while (!stream.isEOF()) {
        val char = stream.readNextChar()
        if (char == '\n') break
        else result.append(char)
    }

    return result.toString()
}
