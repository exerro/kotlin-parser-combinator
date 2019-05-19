
sealed class RegexExpr {
    data class RegexText(val text: String): RegexExpr() { override fun toString() = text }
    data class RegexAlternation(val regexes: Set<RegexExpr>): RegexExpr() { override fun toString() = "(${regexes.joinToString("|") { it.toString() }})" }
    data class RegexConcatenation(val a: RegexExpr, val b: RegexExpr): RegexExpr() { override fun toString() = "$a$b" }
    data class RegexRepetition(val a: RegexExpr, val greedy: Boolean = true): RegexExpr() { override fun toString() = "($a)*" }
    object RegexEOF: RegexExpr() { override fun toString() = "$" }
    object RegexWildcard: RegexExpr() { override fun toString() = "." }
}

val regexText: Parser<RegexExpr, Unit>
        = Parsing.token<Unit>(TOKEN_STR) map { RegexExpr.RegexText(it.text) as RegexExpr }

val regexCharSet
        = Parsing.symbol<Unit>("[") then
          Parsing.symbol<Unit>("^").optional() bindIn { isNegated ->
              Parsing.list(Parsing.token<Unit>(TOKEN_STR) map { RegexExpr.RegexText(it.text) }) map {
                  RegexExpr.RegexAlternation(it.toSet()) as RegexExpr
              }
          } followedBy
          Parsing.symbol("]")

val regexPrimary
        = Parsing.branch(
            Parsing.symbol<Unit>("[") to regexCharSet,
            Parsing.symbol<Unit>("(") to (Parsing.symbol<Unit>("(") then Parsing.defer { regexExpr } followedBy Parsing.symbol(")")),
            Parsing.symbol<Unit>("$") to (Parsing.symbol<Unit>("$") map { RegexExpr.RegexEOF as RegexExpr }),
            Parsing.symbol<Unit>(".") to (Parsing.symbol<Unit>(".") map { RegexExpr.RegexWildcard as RegexExpr }),
            Parsing.token<Unit>(TOKEN_STR)     to regexText
        )

val regexUnary
        = regexPrimary bindIn { primary ->
            Parsing.list(Parsing.symbol<Unit>("+") or Parsing.symbol("-") or Parsing.symbol("*") or Parsing.symbol("?")) map {
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
        = regexUnary bindIn { first -> Parsing.list(regexUnary) map { it.fold(first) { expr, next -> RegexExpr.RegexConcatenation(expr, next) } } }

val regexExpr: Parser<RegexExpr, Unit>
        = regexList sepBy Parsing.symbol("|") map { if (it.size == 1) it[0] else RegexExpr.RegexAlternation(it.toSet()) }

val regexRootExpr: Parser<RegexExpr, Unit>
        = Parsing.branch(
            Parsing.symbol<Unit>("^") to (Parsing.symbol<Unit>("^") then regexExpr followedBy Parsing.token(TOKEN_EOF)),
            Parsing.any<Unit>()                to (regexExpr map { RegexExpr.RegexConcatenation(RegexExpr.RegexRepetition(RegexExpr.RegexWildcard, false), it) as RegexExpr } followedBy Parsing.token(TOKEN_EOF))
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
        Token(TOKEN_STR, if (next != null) chr.toString() + next else chr.toString(), if (next != null) position.extendTo(2) else position)
    }
    else {
        Token(TOKEN_STR, stream.readNextChar().toString(), position)
    }
}

val regexConsumeWhitespace: (TextStream, Position) -> Position = { _, pos -> pos }
