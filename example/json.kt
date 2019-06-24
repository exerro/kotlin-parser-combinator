
sealed class JSONValue
data class JSONString(val value: String): JSONValue()
data class JSONNumber(val value: Float): JSONValue()
data class JSONObject(val entries: Map<String, JSONValue>): JSONValue()
data class JSONArray(val values: List<JSONValue>): JSONValue()
data class JSONBoolean(val value: Boolean): JSONValue()
object JSONNull: JSONValue()

val jsonValueParser: P<JSONValue> = parser { p { branch(
        string to (token map { JSONString(it.text) }),
        oneOf(number, integer) to (token map { JSONNumber(it.text.toFloat()) }),
        keyword("true") to (token map { JSONBoolean(true) }),
        keyword("false") to (token map { JSONBoolean(false) }),
        keyword("null") to (token map { JSONNull }),
        symbol("{") to jsonObjectParser,
        symbol("[") to jsonArrayParser
) } }

val jsonObjectMember = parser {
    text(string) followedBy symbol(":") andThen jsonValueParser
}

val jsonObjectParser = parser {
    wrap(symbol("}") ifNotThen (jsonObjectMember sepBy symbol(",")) defaultsTo listOf(), "{", "}") map {
        JSONObject(it.toMap())
    }
}

val jsonArrayParser = parser {
    wrap(symbol("]") ifNotThen (jsonValueParser sepBy symbol(",")) defaultsTo listOf(), "[", "]") map {
        JSONArray(it)
    }
}

fun jsonLexer(s: TextStream)
        = Lexer(s, LexerTools.keywords(listOf("true", "false", "null")) lexUnion LexerTools.defaults)

fun jsonParse(s: TextStream)
        = jsonValueParser(PState(jsonLexer(s))) .getOr { error(it.getString(s)) }

fun jsonParse(s: String)
        = jsonParse(StringTextStream(s))

fun main() {
    println(jsonParse("\"hi\""))
    println(jsonParse("5"))
    println(jsonParse("5.3"))
    println(jsonParse("true"))
    println(jsonParse("false"))
    println(jsonParse("[1, 2, 3]"))
    println(jsonParse("{\"a\": 4, \"b\": false}"))
    println(jsonParse("{\"a\": 4, \"b\"}"))
}
