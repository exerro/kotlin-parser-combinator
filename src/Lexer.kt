import java.lang.StringBuilder

/** A class used for lexing text streams
 *
 * `consumeToken` consumes a token, given a text stream and position.
 *
 * It may return null to indicate there is no valid token, and may be made from functions in LexerTools.
 */
class Lexer(
        private val stream: TextStream,
        private val consumeToken: (TextStream, Position) -> Token?,
        private val consumeWhitespace: (TextStream, Position) -> Position = defaultConsumeWhitespace,
        private var position: Position = Position(1, 1),
        val lastTokenPosition: Position = Position(1, 0)
) {
    private lateinit var result: Pair<Token, Lexer>

    /** Generates a pair of the next token and the next lexer to use */
    fun next(): Pair<Token, Lexer> {
        if (!::result.isInitialized) {
            position = consumeWhitespace(stream, position)
            val token = if (stream.isEOF()) Token(TOKEN_EOF, "EOF", position) else consumeToken(stream, position)

            if (token != null) {
                result = Pair(token, Lexer(stream, consumeToken, consumeWhitespace, token.getPosition() after 1, token.getPosition()))
            }
            else {
                throw TokenParseException("No token match for character '${stream.peekNextChar()}'")
            }
        }

        return result
    }
}

class TokenParseException(error: String): Throwable(error)

object LexerTools {
    /** Builds a token consumer matching integers */
    fun integers(): (TextStream, Position) -> Token? = matching({ it in '0' .. '9' }) { str, pos ->
        Token(TOKEN_INT, str, pos)
    }

    /** Builds a token consumer matching floating point numbers and integers
     *
     * if intSupported is false, all integers parsed will have type TOKEN_FLOAT */
    fun floats(intSupported: Boolean = true): (TextStream, Position) -> Token? {
        var decimal = false
        return matching({ decimal = false; it in '0'..'9' }, { val r = it in '0'..'9' || !decimal && it == '.'; decimal = decimal || it == '.'; r }) { str, pos ->
            if (str.contains(".") || !intSupported) Token(TOKEN_FLOAT, str, pos) else Token(TOKEN_INT, str, pos)
        }
    }

    /** Builds a token consumer matching identifiers */
    fun identifiers(): (TextStream, Position) -> Token?
            = matching({ it in 'a' .. 'z' || it in 'A' .. 'Z' }, { it in 'a' .. 'z' || it in 'A' .. 'Z' || it in '0' .. '9' }) { str, pos ->
                    Token(TOKEN_IDENT, str, pos)
            }

    /** Builds a token consumer matching keywords and identifiers if supported */
    fun keywords(keywords: Collection<String>, identifiersSupported: Boolean = true): (TextStream, Position) -> Token?
            = { stream, position ->
                val token = identifiers()(stream, position)
                if (token != null && token.text in keywords) Token(TOKEN_KEYWORD, token.text, token.getPosition()) else if (identifiersSupported) token else null
            }

    /** Builds a token consumer matching character literals (e.g. 'a' or '\n') */
    fun chars(): (TextStream, Position) -> Token? {
        var state = 0
        return matching(
                { state = 0; it == '\'' },
                { state < 3 && if (state == 0 && it == '\\') { state = 1; true } else if (state == 0 || state == 1) { state = 2; true } else if (state == 2 && it == '\'') { state = 3; true } else { throw TokenParseException("invalid character in char token") } }
        ) { str, pos ->
            Token(TOKEN_CHAR, str, pos)
        }
    }

    /** Builds a token consumer matching strings and characters
     *
     * if `charSupported` is false, characters will have type TOKEN_STR */
    fun strings(charSupported: Boolean): (TextStream, Position) -> Token? {
        var escaped = true
        var init = '"'
        var finished = false
        return matching(
                { finished = false; escaped = true; init = it; it == '"' || it == '\'' },
                { !finished && (if (escaped) { escaped = false; true } else if (it == init) {finished = true; true} else if (it == '\\') { escaped = true; true } else { true }) }
        ) { str, pos ->
            if (charSupported && init == '\'' && (str.length == 3 || str.length == 4 && str[1] == '\\')) Token(TOKEN_CHAR, str, pos) else Token(TOKEN_STR, str, pos)
        }
    }

    /** Builds a token consumer matching any symbol as a symbol */
    fun any(): (TextStream, Position) -> Token?
            = { stream, position -> Token(TOKEN_SYM, stream.readNextChar().toString(), position) }

    /** Builds a token consumer matching char sequences with an initial condition and further character condition */
    fun matching(init: (Char) -> Boolean, loop: (Char) -> Boolean = init, build: (String, Position) -> Token): (TextStream, Position) -> Token? = { stream, position ->
        if (init(stream.peekNextChar())) {
            val sb = StringBuilder(1)
            sb.append(stream.readNextChar())

            while (!stream.isEOF() && loop(stream.peekNextChar())) sb.append(stream.readNextChar())

            val str = sb.toString()
            val lines = str.split("\n")
            build(str, if (lines.size == 1) position extendTo str.length else position to Position(position.line2 + lines.size - 1, lines[lines.size - 1].length))
        }
        else {
            null
        }
    }
}

/** Builds a token consumer matching either the lvalue or rvalue, with the lvalue taking precedence */
infix fun ((TextStream, Position) -> Token?).lexUnion(
        other: (TextStream, Position) -> Token?): (TextStream, Position
) -> Token? = { stream, position ->
    this(stream, position) ?: other(stream, position)
}

/** Consumes whitespace from the stream */
private val defaultConsumeWhitespace: (TextStream, Position) -> Position = { stream, pos ->
    var position = pos
    while (!stream.isEOF() && (stream.peekNextChar() == ' ' || stream.peekNextChar() == '\t' || stream.peekNextChar() == '\n' || stream.peekNextChar() == '\r')) {
        val symbol = stream.readNextChar()
        position = if (symbol == '\n') Position(position.line2 + 1, 1)
        else position after 1
    }
    position
}
