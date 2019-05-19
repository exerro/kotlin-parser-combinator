
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

inline fun <reified T, reified U>
ParseResultList<T, U>.noErrors(): ParseResultList<T, U>
        = filter { it is ParseResult.ParseSuccess<T, U> } .ifEmpty { this }
