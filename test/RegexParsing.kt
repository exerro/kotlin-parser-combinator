
sealed class RegexExpr {
    data class RegexText(val text: String): RegexExpr() { override fun toString() = text }
    data class RegexAlternation(val regexes: Set<RegexExpr>): RegexExpr() { override fun toString() = "(${regexes.joinToString("|") { it.toString() }})" }
    data class RegexConcatenation(val a: RegexExpr, val b: RegexExpr): RegexExpr() { override fun toString() = "$a$b" }
    data class RegexRepetition(val a: RegexExpr, val greedy: Boolean = true): RegexExpr() { override fun toString() = "($a)*" }
    object RegexEOF: RegexExpr() { override fun toString() = "$" }
    object RegexWildcard: RegexExpr() { override fun toString() = "." }
}

val regexText: Parser<RegexExpr, Unit>
        = ParseTools.token<Unit>(TOKEN_STR) map { RegexExpr.RegexText(it.text) as RegexExpr }

val regexCharSet
        = ParseTools.symbol<Unit>("[") then
          ParseTools.symbol<Unit>("^").optional() bindIn { isNegated ->
              ParseTools.list(ParseTools.token<Unit>(TOKEN_STR) map { RegexExpr.RegexText(it.text) }) map {
                  RegexExpr.RegexAlternation(it.toSet()) as RegexExpr
              }
          } followedBy
          ParseTools.symbol("]")

val regexPrimary
        = ParseTools.branch(
            ParseTools.symbol<Unit>("[") to regexCharSet,
            ParseTools.symbol<Unit>("(") to (ParseTools.symbol<Unit>("(") then ParseTools.defer { regexExpr } followedBy ParseTools.symbol(")")),
            ParseTools.symbol<Unit>("$") to (ParseTools.symbol<Unit>("$") map { RegexExpr.RegexEOF as RegexExpr }),
            ParseTools.symbol<Unit>(".") to (ParseTools.symbol<Unit>(".") map { RegexExpr.RegexWildcard as RegexExpr }),
        ParseTools.token<Unit>(TOKEN_STR)        to regexText
        )

val regexUnary
        = regexPrimary bindIn { primary ->
    ParseTools.list(ParseTools.symbol<Unit>("+") or ParseTools.symbol("-") or ParseTools.symbol("*") or ParseTools.symbol("?")) map {
                it.fold(primary) { expr, sym -> when (sym.text) {
                    "+" -> RegexExpr.RegexConcatenation(expr, RegexExpr.RegexRepetition(expr))
                    "-" -> RegexExpr.RegexRepetition(expr, false)
                    "*" -> RegexExpr.RegexRepetition(expr)
                    "?" -> RegexExpr.RegexAlternation(setOf(expr, RegexExpr.RegexText("")))
                    else -> error("what")
                } }
            }
        }

val regexList
        = regexUnary bindIn { first -> ParseTools.list(regexUnary) map { it.fold(first) { expr, next -> RegexExpr.RegexConcatenation(expr, next) } } }

val regexExpr: Parser<RegexExpr, Unit>
        = regexList sepBy ParseTools.symbol("|") map { if (it.size == 1) it[0] else RegexExpr.RegexAlternation(it.toSet()) }

val regexRootExpr: Parser<RegexExpr, Unit>
        = ParseTools.branch(
            ParseTools.symbol<Unit>("^") to (ParseTools.symbol<Unit>("^") then regexExpr followedBy ParseTools.token(TOKEN_EOF)),
        ParseTools.any<Unit>()                to (regexExpr map { RegexExpr.RegexConcatenation(RegexExpr.RegexRepetition(RegexExpr.RegexWildcard, false), it) as RegexExpr } followedBy ParseTools.token(TOKEN_EOF))
        )

fun regexLexer(stream: TextStream): Lexer = Lexer(
        stream,
        regexConsumeToken,
        regexConsumeWhitespace
)

val regexConsumeToken: (TextStream, Position) -> Token? = { stream, position ->
    if (stream.peekNextChar() in setOf('(', ')', '[', ']', '+', '-', '*', '?', '^', '$', '|', '.')) {
        Token(TOKEN_SYM, stream.readNextChar().toString(), position)
    }
    else if (stream.peekNextChar() == '\\') {
        val chr = stream.readNextChar()
        val next = if (stream.isEOF()) null else stream.readNextChar()
        Token(TOKEN_STR, if (next != null) chr.toString() + next else chr.toString(), if (next != null) position extendTo 2 else position)
    }
    else {
        Token(TOKEN_STR, stream.readNextChar().toString(), position)
    }
}

val regexConsumeWhitespace: (TextStream, Position) -> Position = { _, pos -> pos }
