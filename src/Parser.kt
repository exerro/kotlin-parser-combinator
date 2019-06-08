
typealias Parser<T, U> = (ParseContext<U>) -> ParseResultList<T, U>
typealias ParseResultList<T, U> = List<ParseResult<T, U>>

sealed class ParseResult<out T, out U> {
    data class ParseSuccess<out T, out U>(val value: T, val context: ParseContext<U>): ParseResult<T, U>()
    data class ParseFailure(val error: ParseError): ParseResult<Nothing, Nothing>()
}

data class ParseContext<out U>(val data: U, val lexer: Lexer) {
    fun <R> withValue(value: R): ParseContext<R> = ParseContext(value, lexer)
    fun withLexer(lexer: Lexer): ParseContext<U> = ParseContext(data, lexer)
}

fun <T, U, R> ParseResultList<T, U>.bind(func: (ParseResult.ParseSuccess<T, U>) -> ParseResultList<R, U>): ParseResultList<R, U>
        = map { when (it) {
            is ParseResult.ParseSuccess<T, U> -> func(it)
            is ParseResult.ParseFailure -> listOf(ParseResult.ParseFailure(it.error))
        } } .flatten()

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

fun <T, U> Parser<T, U>.filterErrors(noErrorsIfSuccessful: Boolean = false): Parser<T, U> = { ctx ->
    val results = this(ctx)
    val lastErrorPosition = getLastErrorPosition(results.filter { it is ParseResult.ParseFailure } .map { (it as ParseResult.ParseFailure).error.position })
    val errors = results .filter { it is ParseResult.ParseFailure } .map { it as ParseResult.ParseFailure } .filter {
        (it.error.position.line1 > lastErrorPosition.line1 || it.error.position.line1 == lastErrorPosition.line1 && it.error.position.char1 >= lastErrorPosition.char1)
       }
    if (noErrorsIfSuccessful) {
        results.filter { it is ParseResult.ParseSuccess } .ifEmpty { errors }
    }
    else {
        results.filter { it is ParseResult.ParseSuccess } + errors
    }
}

private fun getLastErrorPosition(positions: List<Position>): Position = positions.last()
