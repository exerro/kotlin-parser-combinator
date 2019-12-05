import java.io.BufferedInputStream
import java.io.File

/** A stream of text to be fed into a lexer */
@Deprecated("Use ASTify instead")
abstract class TextStream {
    /** Read the next character without consuming it */
    abstract fun peekNextChar(): Char

    /** Read the next character and consume it */
    abstract fun readNextChar(): Char

    /** Returns true if there are no more characters to read, false otherwise */
    abstract fun isEOF(): Boolean

    /** Resets the stream (essentially moving it back to position 0) */
    abstract fun reset()
}

/** A text stream created from some text */
@Deprecated("Use ASTify instead")
class StringTextStream(private val content: String): TextStream() {
    private var position = 0

    override fun peekNextChar(): Char {
        if (position < content.length) {
            return content.toCharArray()[position]
        }
        else {
            throw IllegalStateException("failed to peek next character")
        }
    }

    override fun readNextChar(): Char {
        if (position < content.length) {
            return content.toCharArray()[position++]
        }
        else {
            throw IllegalStateException("failed to peek next character")
        }
    }

    override fun isEOF(): Boolean
            = position >= content.length

    override fun reset() {
        position = 0
    }
}

/** A text stream created from the contents of a file */
@Deprecated("Use ASTify instead")
class FileTextStream(private val file: String): TextStream() {
    private var inputStream = BufferedInputStream(File(file).inputStream())

    override fun peekNextChar(): Char {
        if (inputStream.available() != 0) {
            inputStream.mark(1)
            val char = inputStream.read().toChar()
            inputStream.reset()
            return char
        }
        else {
            error("failed to peek next character")
        }
    }

    override fun readNextChar(): Char
            = if (inputStream.available() != 0) inputStream.read().toChar() else error("failed to read next character")

    override fun isEOF(): Boolean
            = inputStream.available() == 0

    override fun reset() {
        inputStream = BufferedInputStream(File(file).inputStream())
    }
}
