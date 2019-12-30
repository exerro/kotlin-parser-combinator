import astify.P
import astify.TextStream
import astify.Token
import astify.parse
import astify.util.TP
import astify.util.TokenParser
import astify.util.lexerParser
import astify.util.tokenP

sealed class JSONValue
data class JSONString(val value: String): JSONValue()
data class JSONNumber(val value: Float): JSONValue()
data class JSONObject(val entries: Map<String, JSONValue>): JSONValue()
data class JSONArray(val values: List<JSONValue>): JSONValue()
data class JSONBoolean(val value: Boolean): JSONValue()
object JSONNull: JSONValue()

//////////////////////////////////////////////////////////////////////////////////////////

internal val jsonValueParser: TP<JSONValue> = tokenP { branch(
        string to (string map { JSONString(it.value) }),
        numeric to numeric,
        symbol("-") to (symbol("-") keepRight numeric map { JSONNumber(-it.value) }),
        keyword("true") to (keyword("true") map { JSONBoolean(true) }),
        keyword("false") to (keyword("false") map { JSONBoolean(false) }),
        keyword("null") to (keyword("null") map { JSONNull }),
        symbol("{") to lazy { jsonObjectParser },
        symbol("[") to lazy { jsonArrayParser }
) }

internal val TokenParser.numeric get()
= number map { JSONNumber(it.value) } or (integer map { JSONNumber(it.value.toFloat()) })

internal val jsonObjectMemberParser: TP<Pair<String, JSONValue>> = tokenP {
    string map { it.value } keepLeft symbol(":") and jsonValueParser
}

internal val jsonObjectParser: TP<JSONValue> = tokenP {
    wrappedCommaSeparated("{", "}", jsonObjectMemberParser) map { JSONObject(it.toMap()) }
}

internal val jsonArrayParser: TP<JSONValue> = tokenP {
    wrappedCommaSeparated("[", "]", jsonValueParser) map { JSONArray(it) } }

//////////////////////////////////////////////////////////////////////////////////////////

val jsonLexer = lexerParser(setOf("true", "false", "null"))

fun jsonParse(s: TextStream) = parse(s, jsonLexer, jsonValueParser)
fun jsonParse(s: String) = parse(s, jsonLexer, jsonValueParser)

//////////////////////////////////////////////////////////////////////////////////////////

fun main() {
    println(jsonParse("\"hi\""))
    println(jsonParse("5"))
    println(jsonParse("5.3"))
    println(jsonParse("true"))
    println(jsonParse("false"))
    println(jsonParse("[1, 2, 3]"))
    println(jsonParse("{\"a\": 4, \"b\": false}"))
    println(jsonParse("{\"a\": 4, \"b\"}"))
    // above ^ should error at the '}' saying it's expecting a ':'
}

//////////////////////////////////////////////////////////////////////////////////////////

private fun <T> TokenParser.wrappedCommaSeparated(s: String, e: String, term: P<Token, T>)
        = symbol(s) keepRight symbol(e) map { listOf<T>() } or
        wrap(term sepBy symbol(","), symbol(s), symbol(e))
