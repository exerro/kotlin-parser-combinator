package astify.util

import astify.Position
import astify.Token
import astify.P
import astify.Parser
import astify.tokenP

sealed class Expr(val position: Position)
sealed class Op

object AddOp: Op(), InfixOperator<Expr> {
    override fun apply(lvalue: Expr, rvalue: Expr) = Add(lvalue, rvalue)
}

class UnmOp(val position: Position): Op(), UnaryOperator<Expr> {
    override fun apply(value: Expr) = Unm(position to value.position, value)
}

open class BinaryOperation(val lvalue: Expr, val rvalue: Expr): Expr(lvalue.position to rvalue.position)
open class UnaryOperation(position: Position, val value: Expr): Expr(position)

class Value(val value: Int, position: Position): Expr(position)
class Add(lvalue: Expr, rvalue: Expr): BinaryOperation(lvalue, rvalue)
class Unm(position: Position, value: Expr): UnaryOperation(position, value)

fun <Token, Expr> Parser<Token>.expressionParser(
        primary: P<Token, Expr>,
        fn: ExpressionParserBuilder<Token, Expr>.() -> Unit
): P<Token, Expr> {
    val builder = ExpressionParserBuilder(primary)
    fn(builder)
    return builder.parser
}

class ExpressionParserBuilder<Token, Expr>(
        private val primary: P<Token, Expr>
) {
    fun infixl(precedence: Int, p: P<Token, InfixOperator<Expr>>) {
        infixes.add(Triple(precedence, false, p))
    }

    fun infixr(precedence: Int, p: P<Token, InfixOperator<Expr>>) {
        infixes.add(Triple(precedence, true, p))
    }

    fun unaryl(precedence: Int, p: P<Token, UnaryOperator<Expr>>) {
        unaries.add(Triple(precedence, false, p))
    }

    fun unaryr(precedence: Int, p: P<Token, UnaryOperator<Expr>>) {
        unaries.add(Triple(precedence, true, p))
    }

    private val unaries: MutableList<Triple<Int, Boolean, P<Token, UnaryOperator<Expr>>>> = mutableListOf()
    private val infixes: MutableList<Triple<Int, Boolean, P<Token, InfixOperator<Expr>>>> = mutableListOf()

    private val luop by lazy { unaries
            .filter { !it.second }
            .map { it.first to it.third } }
    private val ruop by lazy { unaries
            .filter { it.second }
            .map { it.first to it.third } }
    private val bop: P<Token, ParsedInfixOperator<Expr>> by lazy {
        P<Token, ParsedInfixOperator<Expr>> {
            oneOf(infixes.map { (precedence, rassoc, p) -> p map { Triple(precedence, rassoc, it) } })
        }
    }
    private val term: P<Token, Term<Expr>> by lazy { P.seq<Token, Term<Expr>> {
        val (l) = many(opList(luop))
        val (p) = primary
        val (r) = many(opList(ruop))

        Triple(l, p, r)
    } }
    internal val parser: P<Token, Expr> by lazy { P.seq<Token, Expr> {
        val (t0) = term
        val terms = mutableListOf(t0)
        val operators = mutableListOf<ParsedInfixOperator<Expr>>()

        while (true) {
            val (operator) = optional(bop)

            if (operator == null) break
            else TODO()
        }

        TODO()
    } }

    private fun <T> Parser<Token>.opList(ops: List<Pair<Int, P<Token, T>>>)
        = oneOf(ops.map { (precedence, p) -> p map { precedence to it } })
}

interface InfixOperator<Expr> {
    fun apply(lvalue: Expr, rvalue: Expr): Expr
}

interface UnaryOperator<Expr> {
    fun apply(value: Expr): Expr
}

private typealias ParsedUnaryOperators<Expr> = List<Pair<Int, UnaryOperator<Expr>>>
private typealias ParsedInfixOperator<Expr> = Triple<Int, Boolean, InfixOperator<Expr>>
private typealias Term<Expr> = Triple<ParsedUnaryOperators<Expr>, Expr, ParsedUnaryOperators<Expr>>

fun main() {
    tokenP<Any?> {
        expressionParser<Token, Expr>(value { Value(0, Position.start) }) {
            infixl(4, symbol("+") map { AddOp })
            unaryl(6, positioned(symbol("-")) map { p -> UnmOp(p.position) })
        }
    }
}
