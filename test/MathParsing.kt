
val mathOperators = setOf(
        Operator.LeftUnaryOperator(3, "-"),
        Operator.RightUnaryOperator(1, ">"), // adds 1 because fuck it
        Operator.BinaryOperator(1, "+"),
        Operator.BinaryOperator(1, "-"),
        Operator.BinaryOperator(2, "*"),
        Operator.BinaryOperator(4, "^")
)

val termParser = ParseTools.token<Unit>(TOKEN_INT) map { it.text.toInt() }
val mathParser = MathOperatorParser().getParser()

class MathOperatorParser: OperatorParser<Int, Unit>(mathOperators, termParser) {
    override fun getParseState(): OperatorParseState<Int> = MathOperatorParseState()
}

class MathOperatorParseState(operands: MutableList<Int> = ArrayList(), operators: MutableList<Positioned<Operator>> = ArrayList()): OperatorParseState<Int>(operands, operators) {
    override fun copy(): OperatorParseState<Int> = MathOperatorParseState(ArrayList(operands), ArrayList(operators))

    override fun collapseLeftUnaryOperator(operator: Positioned<Operator.LeftUnaryOperator>, value: Int): Int
            = when (operator.getValue().symbol) {
                "-" -> -value
                else -> error("what")
            }

    override fun collapseRightUnaryOperator(operator: Positioned<Operator.RightUnaryOperator>, value: Int): Int
            = when (operator.getValue().symbol) {
                ">" -> value + 1
                else -> error("what")
            }

    override fun collapseBinaryOperator(operator: Positioned<Operator.BinaryOperator>, lvalue: Int, rvalue: Int): Int {
        return when (operator.getValue().symbol) {
            "+" -> lvalue + rvalue
            "-" -> lvalue - rvalue
            "*" -> lvalue * rvalue
            "^" -> Math.pow(lvalue.toDouble(), rvalue.toDouble()).toInt()
            else -> error("what")
        }
    }
}
