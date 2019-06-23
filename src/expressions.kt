
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
        val (firstLOps, firstT, firstROps) = first
        val stack = OperandStack(firstT, collapser)
        firstLOps.forEach { (f, s) -> stack.pushOperator(f, s) }
        firstROps.forEach { (f, s) -> stack.pushOperator(f, s) }
        operations.forEach { (op, operand) ->
            val (lOps, t, rOps) = operand
            stack.pushOperator(op.first, op.second)
            lOps.forEach { (f, s) -> stack.pushOperator(f, s) }
            stack.pushOperand(t)
            rOps.forEach { (f, s) -> stack.pushOperator(f, s) }
        }
        stack.get()
    } }
}

fun <T> ExpressionParserBuilder<T, Token>.parsers() = converter { operator, keyword ->
    if (keyword) keyword(operator) else symbol(operator)
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
