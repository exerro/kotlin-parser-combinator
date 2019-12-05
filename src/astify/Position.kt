package astify

import kotlin.math.max

data class Position(val first: Int, val second: Int = first) {
    /** Return a position spanning this and other. */
    infix fun to(other: Position) = Position(first, other.second)

    /** Return this position, extended to the length given. */
    infix fun extend(length: Int) = Position(first, first + length - 1)

    /** Return this position, with length 1, moved forward characters. */
    infix fun after(characters: Int) = Position(second + characters)

    /** Return true if this position directly proceeds other. */
    infix fun follows(other: Position) = first == other.second + 1

    /** Return the first character's column number. */
    fun columnNumber1(str: TextStream)
            = str.raw.substring(0, first).lastLine().length + 1

    /** Return the second character's column number. */
    fun columnNumber2(str: TextStream)
            = str.raw.substring(0, second).lastLine().length + 1

    /** Return the first character's line number. */
    fun lineNumber1(str: TextStream)
            = str.raw.substring(0, first).count { it == '\n' } + 1

    /** Return the second character's line number. */
    fun lineNumber2(str: TextStream)
            = str.raw.substring(0, second).count { it == '\n' } + 1

    /** Return the contents of the line containing the first character. */
    fun line1(str: TextStream) = str.raw.lineContainingIndex(first)

    /** Return the contents of the line containing the second character. */
    fun line2(str: TextStream) = str.raw.lineContainingIndex(second)

    /** Return the pair of column numbers for the characters. */
    fun colNumbers(str: TextStream) = columnNumber1(str) to columnNumber2(str)

    /** Return the pair of line numbers for the characters. */
    fun lineNumbers(str: TextStream) = lineNumber1(str) to lineNumber2(str)

    /** Return the pair of contents for the lines containing the characters. */
    fun lines(str: TextStream) = line1(str) to line2(str)

    /** Return a summary of the position, showing line/column numbers. */
    fun summary(str: TextStream): String {
        val (line1, line2) = lineNumbers(str)
        val (char1, char2) = colNumbers(str)
        return if (line1 == line2)
            if (char1 == char2) "[line $line1 col $char1]"
            else "[line $line1 col $char1 .. $char2]"
        else "[line $line1 col $char1 .. line $line2 col $char2]"
    }

    /** Return a string containing pointers to the stream text based on the
     *  position's range.
     *  E.g.
     *  <pre>
     *  {@code
     *    1: hello there
     *             ^^^^^^ ...
     *    2: wonderful world
     *  ...  ^^^^^^^^^
     *  }
     *  <pre>
     *  */
    fun linePointer(str: TextStream): String {
        val (line1, line2) = lineNumbers(str)
        val (col1, col2) = colNumbers(str)

        if (line1 == line2) {
            val ls = line1.toString()
            return "$ls: ${line1(str)}\n" +
                    " ".repeat(ls.length + col1 + 1) +
                    "^".repeat(col2 - col1 + 1)
        }
        else {
            val l1 = line1(str)
            val l2 = line2(str)
            val l1s = line1.toString()
            val l2s = line2.toString()
            val ln = max(2, max(l1s.length, l2s.length))

            return "${l1s.pad(ln)}: $l1\n" +
                    " ".repeat(ln + col1 + 1) +
                    "^".repeat(l1.length - col1 + 2) + " ...\n" +
                    "${l2s.pad(ln)}: $l2\n" +
                    "...".pad(ln) + " " +
                    "^".repeat(col2)
        }
    }

    override fun toString() = "Position($first .. $second)"

    companion object {
        val start = Position(0)
    }
}

//////////////////////////////////////////////////////////////////////////////////////////

private fun String.lastLine() = when (val i = lastIndexOf('\n')) {
    -1 -> this
    else -> substring(i + 1)
}

private fun String.lineContainingIndex(index: Int): String {
    var lineStart = 0

    while (true) {
        val idx = indexOf('\n', lineStart)

        if (idx != -1 && idx < index) lineStart = idx + 1
        else break
    }

    return substring(lineStart,
            indexOf('\n', lineStart).takeIf { it != -1 } ?: length)
}

private fun String.repeat(n: Int): String = when (max(0, n)) {
    0 -> ""
    else -> this + repeat(n - 1)
}

private fun String.pad(n: Int) = " ".repeat(n - length) + this
