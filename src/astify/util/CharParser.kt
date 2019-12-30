package astify.util

import astify.P
import astify.Parser

fun <T> charP(fn: CharParser.() -> CP<T>) = fn(CharParser())

typealias CP<T> = P<Char, T>

open class CharParser: Parser<Char>() {
    val digit = satisfying { it.isDigit() }
            .fmape { e, _ -> e.copy(error = "Digit expected") }
    val letter = satisfying { it.isLetter() }
            .fmape { e, _ -> e.copy(error = "Letter expected") }
    val letterOrDigit = satisfying { it.isLetterOrDigit() }
            .fmape { e, _ -> e.copy(error = "Letter or digit expected") }
    val whitespace = many(satisfying { it.isWhitespace() })
    val integer = many(1, digit)
            .map { String(it.toCharArray()).toInt() }
    val identifier = (letter and many(letterOrDigit))
            .map { (x, xs) -> x + String(xs.toCharArray()) }
    val float = (integer and ((equalTo('.')) keepRight integer))
            .map { (intPart, fractionalPart) -> "$intPart.$fractionalPart".toFloat() }
    val charEscaped = ((equalTo('\\')) keepRight anything)
            .map { when (it) { 'n' -> '\n'; else -> it } }
    val stringCharUnescaped = satisfying { it != '"' && it != '\\' }
    val charUnescaped = satisfying { it != '\'' && it != '\\' }
    val stringChar = charEscaped or stringCharUnescaped
    val char = wrap(charEscaped or charUnescaped, equalTo('\''))
    val string = wrap(many(stringChar), equalTo('"')) map { String(it.toCharArray()) }
}