package tutorial

import astify.*
import astify.util.TP
import astify.util.lexerParser
import astify.util.tokenP

typealias Block = Pair<SymbolToken, SymbolToken>

val expressionParser: TP<IntegerToken>
        = tokenP { integer }

val blockParser: TP<Block>
        = tokenP { symbol("{") and symbol("}") } // not going with anything fancy here

data class IfStatement(
        val condition: Token,
        val block: Block,
        val elseBlock: Block?
)

val ifStatementParser: TP<IfStatement> = P.seq {
    val conditionParser = tokenP { wrap(expressionParser, symbol("("), symbol(")")) }
    val (condition) = tokenP { keyword("if") keepRight conditionParser }
    val (block) = blockParser

    if (parse(tokenP { keyword("else") })) {
        val (elseBlock) = blockParser
        IfStatement(condition, block, elseBlock)
    }
    else
        IfStatement(condition, block, null)
}

fun sequencesParserTest() {
    val keywords = setOf("if", "else")
    val lexer = lexerParser(keywords)

    println(parse("if (5) {} else {}", lexer, ifStatementParser))
}
