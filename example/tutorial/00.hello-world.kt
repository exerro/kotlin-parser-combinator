package tutorial

import astify.KeywordToken
import astify.parse
import astify.util.TP
import astify.util.lexerParser
import astify.util.tokenP

val tokenParser: TP<Pair<KeywordToken, KeywordToken>> = tokenP {
    keyword("hello") and keyword("world")
}

fun helloWorldParserTest() {
    val keywords = setOf("hello", "world")
    val lexer = lexerParser(keywords)

    println(parse("hello world", lexer, tokenParser))
    println(parse("hello", lexer, tokenParser)) // this will throw an error
}
