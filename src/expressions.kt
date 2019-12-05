
/** A joiner is a function "joining" a list of expressions and an operator into
 *  a single expression.
 *  Parameters are (operator name, operator object, operands) */
typealias Joiner<Expr, Op> = (String, Op, List<Expr>) -> Expr

fun <Expr, Op> expressionParser(
        term: P<Expr>,
        fn: ExpressionParserBuilder<Expr, Op>.() -> Any?
): P<Expr> {
    val b = ExpressionParserBuilder<Expr, Op>(term)
    fn(b)
    return b.parser()
}

class ExpressionParserBuilder<Expr, Op> internal constructor(
        private val term: P<Expr>
) {
    fun collapse(fn: Joiner<Expr, Op>) {
        collapser = fn
    }

    fun converter(fn: parser.(String, Boolean) -> P<Op>) {
        converter = fn
    }

    /** Declare a left associative infix binary operator. */
    fun infixl(name: String, precedence: Int = 0, getParser: parser.() -> P<Op>) {
        operators.add(Operator(name, getParser(parser), precedence, true, 2))
    }

    /** Declare a right associative infix binary operator. */
    fun infixr(name: String, precedence: Int = 0, getParser: parser.() -> P<Op>) {
        operators.add(Operator(name, getParser(parser), precedence, false, 2))
    }

    /** Declare a left-handed unary operator. */
    fun unaryl(name: String, precedence: Int = 0, getParser: parser.() -> P<Op>) {
        operators.add(Operator(name, getParser(parser), precedence, false, 1))
    }

    /** Declare a right-handed unary operator. */
    fun unaryr(name: String, precedence: Int = 0, getParser: parser.() -> P<Op>) {
        operators.add(Operator(name, getParser(parser), precedence, true, 1))
    }

    /** Declare a left associative infix binary operator. */
    fun infixl(name: String, operator: String, precedence: Int = 0, keyword: Boolean = false): Unit
            = infixl(name, precedence) { converter(operator, keyword) }

    /** Declare a right associative infix binary operator. */
    fun infixr(name: String, operator: String, precedence: Int = 0, keyword: Boolean = false): Unit
            = infixr(name, precedence) { converter(operator, keyword) }

    /** Declare a left-handed unary operator. */
    fun unaryl(name: String, operator: String, precedence: Int = 0, keyword: Boolean = false): Unit
            = unaryl(name, precedence) { converter(operator, keyword) }

    /** Declare a right-handed unary operator. */
    fun unaryr(name: String, operator: String, precedence: Int = 0, keyword: Boolean = false): Unit
            = unaryr(name, precedence) { converter(operator, keyword) }

    internal fun parser(): P<Expr> = parser { opt sepByOp bop map { (first, operations) ->
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

    private val operators = mutableSetOf<Operator<Op>>()
    private lateinit var collapser: Joiner<Expr, Op>
    private lateinit var converter: parser.(String, Boolean) -> P<Op>
    private val bop by lazy { parser { oneOf(operators.filter { it.isInfix } .map { it.parser map { result -> Pair(result, it) } }) } }
    private val lop by lazy { parser { oneOf(operators.filter { it.isUnaryL } .map { it.parser map { result -> Pair(result, it) } }) } }
    private val rop by lazy { parser { oneOf(operators.filter { it.isUnaryR } .map { it.parser map { result -> Pair(result, it) } }) } }
    private val opt by lazy { parser { many(lop) bind { lops -> term bind { t -> many(rop) map { Triple(lops, t, it) } } } } }
}

/** Sets a default converter for the parser builder using `keyword`/`symbol`
 *  depending on the operator. */
fun <T> ExpressionParserBuilder<T, Token>.defaultConverter() = converter { operator, keyword ->
    if (keyword) keyword(operator) else symbol(operator)
}

private class OperandStack<T, O>(
        operand: T,
        val collapser: Joiner<T, O>
) {
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
