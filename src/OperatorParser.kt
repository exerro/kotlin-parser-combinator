
typealias OperatorSet = Set<Operator>

private typealias UnaryTerm<T> = Triple<List<Operator.LeftUnaryOperator>, T, List<Operator.RightUnaryOperator>>
private typealias BinaryTerm<T> = Pair<UnaryTerm<T>, List<Pair<Operator.BinaryOperator, UnaryTerm<T>>>>

abstract class OperatorParser<T, U>(val operators: OperatorSet, val primaryParser: Parser<T, U>) {
    open fun getUnaryTermParser(): Parser<UnaryTerm<T>, U> {
        val lp = getLeftUnaryOperatorParser()
        val rp = getRightUnaryOperatorParser()

        return ParseTools.list(lp) bindIn { lops ->
            primaryParser bindIn { value ->
                ParseTools.list(rp) bindIn { rops ->
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
                    ParseTools.value<Pair<Operator.BinaryOperator, UnaryTerm<T>>, U>(Pair(operator, term))
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

    open fun getBinaryOperatorParser(): Parser<Operator.BinaryOperator, U>
            = operators
            .filter { it is Operator.BinaryOperator }
            .map { it as Operator.BinaryOperator }
            .map { operator -> getParserForOperator(operator) map { operator } }
            .union()

    open fun getRightUnaryOperatorParser(): Parser<Operator.RightUnaryOperator, U>
            = operators
            .filter { it is Operator.RightUnaryOperator }
            .map { it as Operator.RightUnaryOperator }
            .map { operator -> getParserForOperator(operator) map { operator } }
            .union()

    open fun getLeftUnaryOperatorParser(): Parser<Operator.LeftUnaryOperator, U>
        = operators
            .filter { it is Operator.LeftUnaryOperator }
            .map { it as Operator.LeftUnaryOperator }
            .map { operator -> getParserForOperator(operator) map { operator } }
            .union()

    protected fun getParserForOperator(operator: Operator)
        = if (operator.isKeyword) ParseTools.keyword<U>(operator.symbol) else ParseTools.symbol(operator.symbol)

    abstract fun getParseState(): OperatorParseState<T>
}

abstract class OperatorParseState<T>(
        protected val operands: MutableList<T> = ArrayList(),
        protected val operators: MutableList<Operator> = ArrayList()
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

    fun pushOperator(operator: Operator): OperatorParseState<T> {
        val new = copy()
        while (new.operators.isNotEmpty() && new.shouldCollapseBefore(operator)) new.collapse()
        new.rawPushOperator(operator)
        return new
    }

    abstract fun copy(): OperatorParseState<T>
    abstract fun collapseLeftUnaryOperator(operator: Operator.LeftUnaryOperator, value: T): T
    abstract fun collapseRightUnaryOperator(operator: Operator.RightUnaryOperator, value: T): T
    abstract fun collapseBinaryOperator(operator: Operator.BinaryOperator, lvalue: T, rvalue: T): T

    private fun rawPushOperand(operand: T) { operands.add(operand) }
    private fun rawPopOperand(): T = operands.removeAt(operands.size - 1)
    private fun rawPushOperator(operator: Operator) { operators.add(operator) }
    private fun rawPopOperator(): Operator = operators.removeAt(operators.size - 1)
    private fun peekOperator(): Operator = operators[operators.size - 1]

    private fun collapse() {
        if (operators.isNotEmpty()) {
            when (val operator = rawPopOperator()) {
                is Operator.LeftUnaryOperator -> {
                    val value = rawPopOperand()
                    rawPushOperand(collapseLeftUnaryOperator(operator, value))
                }
                is Operator.RightUnaryOperator -> {
                    val value = rawPopOperand()
                    rawPushOperand(collapseRightUnaryOperator(operator, value))
                }
                is Operator.BinaryOperator -> {
                    val rvalue = rawPopOperand()
                    val lvalue = rawPopOperand()
                    rawPushOperand(collapseBinaryOperator(operator, lvalue, rvalue))
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
    open class LeftUnaryOperator(precedence: Int, symbol: String, isKeyword: Boolean = false): Operator(precedence, symbol, isKeyword)
    open class RightUnaryOperator(precedence: Int, symbol: String, isKeyword: Boolean = false): Operator(precedence, symbol, isKeyword)
    open class BinaryOperator(precedence: Int, symbol: String, val leftAssociative: Boolean = true, isKeyword: Boolean = false): Operator(precedence, symbol, isKeyword)
}
