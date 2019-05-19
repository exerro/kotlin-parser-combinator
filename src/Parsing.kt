
object Parsing {
    fun <U> nothing(): Parser<Unit, U> = { context ->
        listOf(ParseResult.ParseSuccess(Unit, context))
    }

    fun <U> any(): Parser<Token, U> = { context ->
        val (token, lexer) = context.lexer.next()
        listOf(ParseResult.ParseSuccess(token, context.withLexer(lexer)))
    }

    fun <U> token(type: TokenType): Parser<Token, U>
            = any<U>() maybeError { it, _ ->
                if (it.type == type) { null }
                else { ParseError("Expected any $type, got ${it.type} '${it.text}'", it.getPosition()) }
            }

    fun <U> symbol(symbol: String): Parser<Token, U>
            = any<U>() maybeError { it, _ ->
                if (it.type == TOKEN_SYM && it.text == symbol) { null }
                else { ParseError("Expected symbol '$symbol', got ${it.type} '${it.text}'", it.getPosition()) }
            }

    fun <T, U> defer(generator: () -> Parser<T, U>): Parser<T, U> = { ctx -> generator()(ctx) }

    fun <T, U> list(parser: Parser<T, U>): Parser<List<T>, U>
            = parser bindIn { first -> list(parser) map { listOf(first) + it } } union (nothing<U>() map { listOf<T>() })

    fun <T, U> branch(vararg parsers: Pair<Parser<*, U>, Parser<T, U>>)
            = { ctx: ParseContext<U> ->
                val errors = HashSet<ParseError>()
                var result: ParseResultList<T, U>? = null

                for (parser in parsers) {
                    val results = parser.first(ctx)
                    if (results.any { it is ParseResult.ParseSuccess }) {
                        result = parser.second(ctx)
                        break
                    }
                    else {
                        errors.addAll(results.map { (it as ParseResult.ParseFailure).error })
                    }
                }

                result ?: listOf(ParseResult.ParseFailure(ParseError("No viable alternatives", ctx.lexer.next().first.getPosition(), errors)))
            }
}

fun <T, U> Parser<T, U>.single(): Parser<T, U> = { ctx ->
    val results = this(ctx)
    results .filter { it is ParseResult.ParseSuccess } .take(1) + results.filter { it is ParseResult.ParseFailure }
}

fun <T, U> Parser<T, U>.optional(): Parser<T?, U> = this or (Parsing.nothing<U>() map { null })

infix fun <T, U> Parser<T, U>.filter(predicate: (T, ParseContext<U>) -> Boolean): Parser<T, U> = { ctx ->
    this(ctx).bind { if (predicate(it.value, it.context)) listOf(it) else listOf() }
}

infix fun <T, U> Parser<T, U>.maybeError(error: (T, ParseContext<U>) -> ParseError?): Parser<T, U> = { ctx ->
    this(ctx).bind {
        val e = error(it.value, it.context)
        if (e != null) listOf(ParseResult.ParseFailure(e)) else listOf(it)
    }
}

infix fun <T, U> Parser<T, U>.collectErrors(collector: (Set<ParseError>, Position) -> ParseResultList<T, U>): Parser<T, U> = { ctx ->
    val results = this(ctx)
    val pos = ctx.lexer.next().first.getPosition()
    results.filter { it is ParseResult.ParseSuccess } +
            collector(results.filter { it is ParseResult.ParseFailure } .map { (it as ParseResult.ParseFailure).error } .toSet(), pos)
}

infix fun <T, U> Parser<T, U>.collectErrors(message: String): Parser<T, U> = collectErrors { errors, pos ->
    listOf(ParseResult.ParseFailure(ParseError(message, pos, errors)))
}

fun <T, U> Parser<T, U>.filterErrors(noErrorsIfSuccess: Boolean = false): Parser<T, U> = { ctx ->
    val results = this(ctx)
    val lastErrorPosition = getLastErrorPosition(results.filter { it is ParseResult.ParseFailure } .map { (it as ParseResult.ParseFailure).error.position })
    val errors = results .filter { it is ParseResult.ParseFailure } .map { it as ParseResult.ParseFailure } .filter {
        (it.error.position.line1 > lastErrorPosition.line1 || it.error.position.line1 == lastErrorPosition.line1 && it.error.position.char1 >= lastErrorPosition.char1)
    }
    if (noErrorsIfSuccess) {
        results.filter { it is ParseResult.ParseSuccess } .ifEmpty { errors }
    }
    else {
        results.filter { it is ParseResult.ParseSuccess } + errors
    }
}

infix fun <T, R, U> Parser<T, U>.map(func: (T) -> R): Parser<R, U> = { ctx ->
    this(ctx).bind { result -> listOf(ParseResult.ParseSuccess(func(result.value), result.context)) }
}

infix fun <T, R, U> Parser<T, U>.mapc(func: (T, ParseContext<U>) -> R): Parser<R, U> = { ctx ->
    this(ctx).bind { result -> listOf(ParseResult.ParseSuccess(func(result.value, result.context), result.context)) }
}

infix fun <T, U> Parser<T, U>.or(other: Parser<T, U>): Parser<T, U> = { ctx ->
    val results = this(ctx)
    results.filter { it is ParseResult.ParseSuccess } .ifEmpty {
        val others = other(ctx)
        others.filter { it is ParseResult.ParseSuccess } .ifEmpty { results + others }
    }
}

infix fun <T, U> Parser<T, U>.union(other: Parser<T, U>): Parser<T, U> = { ctx ->
    this(ctx) + other(ctx)
}

infix fun <T, R, U> Parser<T, U>.then(other: Parser<R, U>): Parser<R, U> = { ctx ->
    this.filterErrors()(ctx).bind { first ->
        other(first.context).bind { second ->
            listOf(ParseResult.ParseSuccess(second.value, second.context))
        }
    }
}

infix fun <T, R, U> Parser<T, U>.followedBy(other: Parser<R, U>): Parser<T, U> = { ctx ->
    this.filterErrors()(ctx).bind { first ->
        other(first.context).bind { second ->
            listOf(ParseResult.ParseSuccess(first.value, second.context))
        }
    }
}

infix fun <T, R, U> Parser<T, U>.sepBy(other: Parser<R, U>): Parser<List<T>, U> = this bindIn { first -> { ctx: ParseContext<U> ->
    Parsing.list(other then this)(ctx).bind { listOf(ParseResult.ParseSuccess(listOf(first) + it.value, it.context)) }
} }

infix fun <T, R, U> Parser<T, U>.bindIn(p: (T) -> Parser<R, U>): Parser<R, U> = { ctx ->
    this(ctx).bind { p(it.value)(it.context) }
}

private fun getLastErrorPosition(positions: List<Position>): Position {
    if (positions.isEmpty()) return Position(0, 0)
    var last = positions[0]

    for (position in positions.subList(1, positions.size)) {
        if (position.line1 > last.line1 || position.line1 == last.line1 && position.char1 > last.char1) last = position
    }

    return last
}
