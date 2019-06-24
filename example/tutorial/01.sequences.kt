package tutorial

import parser
import parse
import Token

val expressionParser = parser { integer }
val blockParser = parser { symbol("{") andThen symbol("}") } // not going with anything fancy here

data class IfStatement(
        val condition: Token,
        val block: Pair<Token, Token>,
        val elseBlock: Pair<Token, Token>?
)

val ifStatementParser = parser.sequence {
    p { identifier("if") } // identifier will be replaced with keyword in following tutorials
    p { symbol("(") }
    val condition = p { expressionParser }
    p { symbol(")") }
    val block = p { blockParser }
    val elseBlock = if (p { optional(identifier("else")) } != null) p { blockParser } else null
    IfStatement(condition, block, elseBlock)
}

fun sequencesParserTest() {
    println(ifStatementParser parse "if (5) {} else {}")
}
