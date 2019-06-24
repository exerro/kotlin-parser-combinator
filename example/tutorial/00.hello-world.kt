package tutorial

import parser
import parse

val tokenParser = parser {
    identifier("hello") andThen identifier("world")
}

fun helloWorldParserTest() {
    println(tokenParser parse "hello world")
    println(tokenParser parse "hello") // this will throw an error
}
