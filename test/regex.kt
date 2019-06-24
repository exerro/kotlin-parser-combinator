import java.lang.IllegalStateException
import error as error1

fun parsingRegexTests() {
    test("Parsing regex") {
        // TODO!
    }
}

sealed class RegexExpr {
    data class RegexText(val text: String): RegexExpr() { override fun toString() = text }
    data class RegexAlternation(val regexes: Set<RegexExpr>): RegexExpr() { override fun toString() = "(${regexes.joinToString("|") { it.toString() }})" }
    data class RegexConcatenation(val a: RegexExpr, val b: RegexExpr): RegexExpr() { override fun toString() = "$a$b" }
    data class RegexRepetition(val a: RegexExpr, val greedy: Boolean = true): RegexExpr() { override fun toString() = "($a)*" }
    object RegexEOF: RegexExpr() { override fun toString() = "$" }
    object RegexWildcard: RegexExpr() { override fun toString() = "." }
}

val regexText: P<RegexExpr> = parser {
    tokenType(TOKEN_STR) map { RegexExpr.RegexText(it.text) as RegexExpr }
}

val regexCharSet = parser {
    wrap(sequence {
        val isNegated = p { optional(symbol("^")) }
        val ss = p { tokenType(TOKEN_STR) until symbol("]") }.map {
            RegexExpr.RegexText(it.text)
        }
        RegexExpr.RegexAlternation(ss.toSet())
    }, "[", "]")
}

val regexPrimary = parser { oneOf(
        regexCharSet,
        wrap(p { regexExpr }, "(", ")"),
        symbol("$") map { RegexExpr.RegexEOF },
        symbol(".") map { RegexExpr.RegexWildcard },
        regexText
) }

val regexUnaryOperator = parser { oneOf(
        symbol("+"),
        symbol("-"),
        symbol("*"),
        symbol("?")
) }

val regexUnary = parser { sequence {
    val primary = p { regexPrimary }
    val operators = p { many(regexUnaryOperator) }

    operators.fold(primary) { expr, sym ->
        when (sym.text) {
            "+" -> RegexExpr.RegexConcatenation(expr, RegexExpr.RegexRepetition(expr))
            "-" -> RegexExpr.RegexRepetition(expr, false)
            "*" -> RegexExpr.RegexRepetition(expr)
            "?" -> RegexExpr.RegexAlternation(setOf(expr, RegexExpr.RegexText("")))
            else -> throw IllegalStateException("what")
        }
    }
} }

val regexList = parser { sequence {
    val first = p { regexUnary }
    val rest = p { many(regexUnary) }

    rest.fold(first) { expr, next ->
        RegexExpr.RegexConcatenation(expr, next)
    }
} }

val regexExpr: P<RegexExpr> = parser {
    regexList sepBy symbol("|") map { if (it.size == 1) it[0] else RegexExpr.RegexAlternation(it.toSet()) }
}

val regexRootExpr = parser { sequence {
    if (p { optional(symbol("^")) } != null) {
        p { regexExpr followedBy eof }
    }
    else {
        p { regexExpr followedBy eof map {
            RegexExpr.RegexConcatenation(RegexExpr.RegexRepetition(RegexExpr.RegexWildcard, false), it)
        } }
    }
} }

fun regexLexer(stream: TextStream): Lexer = Lexer(
        stream,
        regexConsumeToken,
        regexConsumeWhitespace
)

val regexConsumeToken: (TextStream, Position) -> Token? = { stream, position ->
    when {
        stream.peekNextChar() in setOf('(', ')', '[', ']', '+', '-', '*', '?', '^', '$', '|', '.') -> {
            Token(TOKEN_SYM, stream.readNextChar().toString(), position)
        }
        stream.peekNextChar() == '\\' -> {
            val chr = stream.readNextChar()
            val next = if (stream.isEOF()) null else stream.readNextChar()
            Token(TOKEN_STR, if (next != null) chr.toString() + next else chr.toString(), if (next != null) position extendTo 2 else position)
        }
        else -> Token(TOKEN_STR, stream.readNextChar().toString(), position)
    }
}

val regexConsumeWhitespace: (TextStream, Position) -> Position = { _, pos -> pos }
