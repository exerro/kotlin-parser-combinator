import java.lang.Math.pow

typealias Collapser<T, O> = (String, O, List<T>) -> T

fun <T, O> expressionParser(term: P<T>, fn: ExpressionParserBuilder<T, O>.() -> Any?): P<T> {
    val b = ExpressionParserBuilder<T, O>(term)
    fn(b)
    b.debug()
    return b.parser()
}

class ExpressionParserBuilder<T, O> internal constructor(val term: P<T>) {
    private val operators = mutableSetOf<Operator<O>>()
    private lateinit var collapser: Collapser<T, O>
    private lateinit var converter: parser.(String, Boolean) -> P<O>
    private val bop by lazy { parser { oneOf(operators.filter { it.isInfix } .map { it.parser map { result -> Pair(result, it) } }) } }
    private val lop by lazy { parser { oneOf(operators.filter { it.isUnaryL } .map { it.parser map { result -> Pair(result, it) } }) } }
    private val rop by lazy { parser { oneOf(operators.filter { it.isUnaryR } .map { it.parser map { result -> Pair(result, it) } }) } }
    private val opt by lazy { parser { many(lop) bind { lops -> term bind { t -> many(rop) map { Triple(lops, t, it) } } } } }

    fun collapse(fn: Collapser<T, O>) {
        collapser = fn
    }

    fun converter(fn: parser.(String, Boolean) -> P<O>) {
        converter = fn
    }

    fun infixl(name: String, precedence: Int = 0, getParser: parser.() -> P<O>) {
        operators.add(Operator(name, getParser(parser), precedence, true, 2))
    }

    fun infixr(name: String, precedence: Int = 0, getParser: parser.() -> P<O>) {
        operators.add(Operator(name, getParser(parser), precedence, false, 2))
    }

    fun unaryl(name: String, precedence: Int = 0, getParser: parser.() -> P<O>) {
        operators.add(Operator(name, getParser(parser), precedence, false, 1))
    }

    fun unaryr(name: String, precedence: Int = 0, getParser: parser.() -> P<O>) {
        operators.add(Operator(name, getParser(parser), precedence, true, 1))
    }

    fun infixl(name: String, operator: String, precedence: Int = 0, keyword: Boolean = false): Unit
            = infixl(name, precedence) { converter(operator, keyword) }

    fun infixr(name: String, operator: String, precedence: Int = 0, keyword: Boolean = false): Unit
            = infixr(name, precedence) { converter(operator, keyword) }

    fun unaryl(name: String, operator: String, precedence: Int = 0, keyword: Boolean = false): Unit
            = unaryl(name, precedence) { converter(operator, keyword) }

    fun unaryr(name: String, operator: String, precedence: Int = 0, keyword: Boolean = false): Unit
            = unaryr(name, precedence) { converter(operator, keyword) }

    internal fun debug() {
        println(operators.filter { it.isUnaryL })
    }

    internal fun parser(): P<T> = parser { opt sepByOp bop map { (first, operations) ->
        val (lops, t, rops) = first
        val stack = OperandStack(t, collapser)
        lops.forEach { (f, s) -> stack.pushOperator(f, s) }
        rops.forEach { (f, s) -> stack.pushOperator(f, s) }
        operations.forEach { (op, operand) ->
            val (lops, t, rops) = operand
            stack.pushOperator(op.first, op.second)
            lops.forEach { (f, s) -> stack.pushOperator(f, s) }
            stack.pushOperand(t)
            rops.forEach { (f, s) -> stack.pushOperator(f, s) }
        }
        stack.get()
    } }
}

fun <T> ExpressionParserBuilder<T, Token>.parsers() = converter { operator, keyword ->
    if (keyword) keyword(operator) else sym(operator)
}

private class OperandStack<T, O>(operand: T, val collapser: Collapser<T, O>) {
    var operands = mutableListOf(operand)
    val operators = mutableListOf<Pair<O, Operator<O>>>()

    fun collapse() {
        val operator = operators.removeAt(operators.size - 1)
        val operands = operands.take(operator.second.operands).reversed()
        this.operands = this.operands.drop(operator.second.operands).toMutableList()
        pushOperand(collapser(operator.second.name, operator.first, operands))
    }

    fun shouldCollapse(op: Operator<O>, newOp: Operator<O>): Boolean {
        if (!op.isUnaryL && newOp.isUnaryL) return false
        if (op.isUnaryR) return true
        if (op.isInfix && newOp.isUnaryL) return true
        if (op.precedence == newOp.precedence && op.leftAssociative) return true
        if (op.precedence > newOp.precedence) return true
        return false
    }

    fun pushOperator(value: O, operator: Operator<O>) {
        while (operators.isNotEmpty() && shouldCollapse(operators.last().second, operator)) collapse()
        operators.add(Pair(value, operator))
    }

    fun pushOperand(operand: T) {
        operands.add(0, operand)
    }

    fun get(): T {
        while (operators.size > 0) collapse()
        return operands[0]
    }
}

private data class Operator<O>(val name: String, val parser: P<O>, val precedence: Int, val leftAssociative: Boolean, val operands: Int) {
    val isUnaryL by lazy { !leftAssociative && operands == 1 }
    val isUnaryR by lazy { leftAssociative && operands == 1 }
    val isInfix by lazy { operands == 2 }
}

val int: P<Int> = parser { integer map { it.text.toInt() } or wrap(p { maths }, "(", ")") }
val maths: P<Int> = parser { expressionParser<Int, Token>(int) {
    parsers()
    unaryl("unm", "-", 6)
    unaryl("lop", "<", 2)
    unaryr("rop", ">", 2)
    infixl("add", "+", 3)
    infixl("sub", "-", 3)
    infixl("mul", "*", 4)
    infixl("div", "/", 4)
    infixr("pow", "^", 5)

    collapse { name, _, operands -> when (name) {
        "unm" -> -operands[0]
        "lop" -> operands[0] - 1
        "rop" -> operands[0] + 1
        "add" -> operands[0] + operands[1]
        "sub" -> operands[0] - operands[1]
        "mul" -> operands[0] * operands[1]
        "div" -> operands[0] / operands[1]
        "pow" -> pow(operands[0].toDouble(), operands[1].toDouble()).toInt()
        else -> error("unsupported operator '$name'")
    } }
} }

val ifStatement = parser { sequence {
    p { keyword("if") }

    val cond = p { wrap(maths, "(", ")") }

    p { keyword("then") }

    val value = p { maths }
    val elseValue = p { optional(keyword("else") then maths) }

    if (cond > 0) value else elseValue
} }

fun main() {
    val s = "if (0) then 1 - 2 * -<3"
    val lexer = LexerTools.keywords(setOf("if", "then", "else")) lexUnion LexerTools.defaults
    parser { ifStatement followedBy eof } (PState(Lexer(StringTextStream(s), lexer)))
            .apply { println("Parsed $it") }
            .onError { println("Errored: ${it.getString { StringTextStream(s) }}") }
}
