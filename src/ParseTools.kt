
object ParseTools {
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

    fun <U> token(type: TokenType, text: String): Parser<Token, U>
            = any<U>() maybeError { it, _ ->
                if (it.type == type && it.text == text) { null }
                else { ParseError("Expected $type '$text', got ${it.type} '${it.text}'", it.getPosition()) }
            }

    fun <T, U> defer(generator: () -> Parser<T, U>): Parser<T, U> = { ctx -> generator()(ctx) }

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

    fun <U> identifier(): Parser<Token, U>
            = token(TOKEN_IDENT)

    fun <U> keyword(keyword: String): Parser<Token, U>
            = token(TOKEN_KEYWORD, keyword)

    fun <U> symbol(symbol: String): Parser<Token, U>
            = token(TOKEN_SYM, symbol)

    fun <T, U> list(parser: Parser<T, U>): Parser<List<T>, U>
            = parser bindIn { first -> list(parser) map { listOf(first) + it } } union (nothing<U>() map { listOf<T>() })

    fun <T, U> oneOrMore(parser: Parser<T, U>): Parser<List<T>, U>
            = parser bindIn { first -> list(parser) map { listOf(first) + it } }

    fun <U> modifier(keyword: String): Parser<Boolean, U>
            = keyword<U>(keyword).optional().single() map { it != null }

    fun <T, U> value(value: T): Parser<T, U>
            = { ctx -> listOf(ParseResult.ParseSuccess(value, ctx)) }
}

fun <T, U> Parser<T, U>.optional(): Parser<T?, U>
        = this or (ParseTools.nothing<U>() map { null })

infix fun <T, R, U> Parser<T, U>.sepBy(other: Parser<R, U>): Parser<List<T>, U> = this bindIn { first -> { ctx: ParseContext<U> ->
    ParseTools.list(other then this)(ctx).bind { listOf(ParseResult.ParseSuccess(listOf(first) + it.value, it.context)) }
} }

fun <T, U> List<Parser<T, U>>.union(): Parser<T, U>
        = this.subList(1, this.size).fold(this[0]) { a, b -> a union b }

fun <T, U> Parser<T, U>.single(): Parser<T, U> = { ctx ->
    val results = this(ctx)
    results .filter { it is ParseResult.ParseSuccess } .take(1) + results.filter { it is ParseResult.ParseFailure }
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

infix fun <T, R, U> Parser<T, U>.bindIn(p: (T) -> Parser<R, U>): Parser<R, U> = { ctx ->
    this(ctx).bind { p(it.value)(it.context) }
}
