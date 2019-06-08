
typealias OperatorSet = Set<Operator>

private typealias UnaryTerm<T> = Triple<List<Positioned<Operator.LeftUnaryOperator>>, T, List<Positioned<Operator.RightUnaryOperator>>>
private typealias BinaryTerm<T> = Pair<UnaryTerm<T>, List<Pair<Positioned<Operator.BinaryOperator>, UnaryTerm<T>>>>

abstract class OperatorParser<T, U>(private val operators: OperatorSet, private val primaryParser: Parser<T, U>) {
    open fun getUnaryTermParser(): Parser<UnaryTerm<T>, U> {
        val lp = getLeftUnaryOperatorParser()
        val rp = getRightUnaryOperatorParser()
        val lpt = if (lp != null) ParseTools.list(lp) else ParseTools.nothing<U>() map { listOf<Positioned<Operator.LeftUnaryOperator>>() }
        val rpt = if (rp != null) ParseTools.list(rp) else ParseTools.nothing<U>() map { listOf<Positioned<Operator.RightUnaryOperator>>() }

        return lpt bindIn { lops ->
            primaryParser bindIn { value ->
                rpt bindIn { rops ->
                    ParseTools.value<UnaryTerm<T>, U>(
                            Triple(lops, value, rops)
                    )
                }
            }
        }
    }

    open fun getBinaryTermParser(): Parser<BinaryTerm<T>, U> {
        val term = getUnaryTermParser()
        val bp = getBinaryOperatorParser()

        return term bindIn { first ->
            ParseTools.list(bp bindIn { operator ->
                term bindIn { term ->
                    ParseTools.value<Pair<Positioned<Operator.BinaryOperator>, UnaryTerm<T>>, U>(Pair(operator, term))
                }
            }) bindIn { operators ->
                ParseTools.value<BinaryTerm<T>, U>(Pair(first, operators))
            }
        }
    }

    fun getParser(): Parser<T, U> {
        return getBinaryTermParser() map { (first, operators) ->
            var state = getParseState()

            fun addTerm(term: UnaryTerm<T>) {
                state = term.third.fold(term.first.fold(state) { s, it -> s.pushOperator(it) } .pushOperand(term.second)) { s, it -> s.pushOperator(it) }
            }

            addTerm(first)

            operators.map {
                state = state.pushOperator(it.first)
                addTerm(it.second)
            }

            state.getResult()
        }
    }

    open fun getBinaryOperatorParser(): Parser<Positioned<Operator.BinaryOperator>, U>
            = operators
            .filter { it is Operator.BinaryOperator }
            .map { it as Operator.BinaryOperator }
            .map { operator -> getParserForOperator(operator) map { it.withValue(operator) } }
            .union()

    open fun getRightUnaryOperatorParser(): Parser<Positioned<Operator.RightUnaryOperator>, U>?
            = operators
            .filter { it is Operator.RightUnaryOperator }
            .map { it as Operator.RightUnaryOperator }
            .map { operator -> getParserForOperator(operator) map { it.withValue(operator) } }
            .ifEmpty { null }
            ?.union()

    open fun getLeftUnaryOperatorParser(): Parser<Positioned<Operator.LeftUnaryOperator>, U>?
        = operators
            .filter { it is Operator.LeftUnaryOperator }
            .map { it as Operator.LeftUnaryOperator }
            .map { operator -> getParserForOperator(operator) map { it.withValue(operator) } }
            .ifEmpty { null }
            ?.union()

    protected fun getParserForOperator(operator: Operator)
        = if (operator.isKeyword) ParseTools.keyword<U>(operator.symbol) else ParseTools.symbol(operator.symbol)

    abstract fun getParseState(): OperatorParseState<T>
}

abstract class OperatorParseState<T>(
        protected val operands: MutableList<T> = ArrayList(),
        protected val operators: MutableList<Positioned<Operator>> = ArrayList()
) {
    fun getResult(): T {
        while (operators.isNotEmpty()) collapse()
        return operands[0]
    }

    fun pushOperand(operand: T): OperatorParseState<T> {
        val new = copy()
        new.rawPushOperand(operand)
        return new
    }

    fun pushOperator(operator: Positioned<Operator>): OperatorParseState<T> {
        val new = copy()
        while (new.operators.isNotEmpty() && new.shouldCollapseBefore(operator.getValue())) new.collapse()
        new.rawPushOperator(operator)
        return new
    }

    abstract fun copy(): OperatorParseState<T>
    abstract fun collapseLeftUnaryOperator(operator: Positioned<Operator.LeftUnaryOperator>, value: T): T
    abstract fun collapseRightUnaryOperator(operator: Positioned<Operator.RightUnaryOperator>, value: T): T
    abstract fun collapseBinaryOperator(operator: Positioned<Operator.BinaryOperator>, lvalue: T, rvalue: T): T

    private fun rawPushOperand(operand: T) { operands.add(operand) }
    private fun rawPopOperand(): T = operands.removeAt(operands.size - 1)
    private fun rawPushOperator(operator: Positioned<Operator>) { operators.add(operator) }
    private fun rawPopOperator(): Positioned<Operator> = operators.removeAt(operators.size - 1)
    private fun peekOperator(): Operator = operators[operators.size - 1].getValue()

    private fun collapse() {
        if (operators.isNotEmpty()) {
            val positionedOperator = rawPopOperator()
            when (val operator = positionedOperator.getValue()) {
                is Operator.LeftUnaryOperator -> {
                    val value = rawPopOperand()
                    rawPushOperand(collapseLeftUnaryOperator(positionedOperator.withValue(operator), value))
                }
                is Operator.RightUnaryOperator -> {
                    val value = rawPopOperand()
                    rawPushOperand(collapseRightUnaryOperator(positionedOperator.withValue(operator), value))
                }
                is Operator.BinaryOperator -> {
                    val rvalue = rawPopOperand()
                    val lvalue = rawPopOperand()
                    rawPushOperand(collapseBinaryOperator(positionedOperator.withValue(operator), lvalue, rvalue))
                }
            }
        }
    }

    private fun shouldCollapseBefore(operator: Operator): Boolean {
        return when (val op = peekOperator()) {
            is Operator.RightUnaryOperator -> true
            is Operator.LeftUnaryOperator  -> operator is Operator.LeftUnaryOperator || (operator.precedence <= op.precedence)
            is Operator.BinaryOperator     -> operator.precedence < op.precedence || operator.precedence == op.precedence && op.leftAssociative
            else -> false
        }
    }
}

sealed class Operator(
        val precedence: Int,
        val symbol: String,
        val isKeyword: Boolean = false
) {
    open class LeftUnaryOperator(precedence: Int, symbol: String, isKeyword: Boolean = false): Operator(precedence, symbol, isKeyword) { override fun toString() = "($symbol)" }
    open class RightUnaryOperator(precedence: Int, symbol: String, isKeyword: Boolean = false): Operator(precedence, symbol, isKeyword) { override fun toString() = "($symbol)" }
    open class BinaryOperator(precedence: Int, symbol: String, val leftAssociative: Boolean = true, isKeyword: Boolean = false): Operator(precedence, symbol, isKeyword) { override fun toString() = "($symbol)" }
}
