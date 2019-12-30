package astify_tests

import Tester
import assertValueEquals
import astify.*
import astify.util.ListParserState
import astify.util.PositionedValue
import astify.util.positioned
import child
import error

fun <T> iP(fn: Parser<Int>.() -> P<Int, T>) = P(fn)

fun gt(n: Int): P<Int, Int> = P {
    satisfying { it > n } fmape { f, _ ->
        f.copy(error = "Expected value greater than $n")
    }
}

fun lt(n: Int): P<Int, Int> = P {
    satisfying { it < n } fmape { f, _ ->
        f.copy(error = "Expected value greater than $n")
    }
}

fun eq(n: Int): P<Int, Int> = P {
    satisfying { it == n } fmape { f, _ ->
        f.copy(error = "Expected value greater than $n")
    }
}

fun gte(n: Int): P<Int, Int> = P { gt(n) or eq(n) }
fun lte(n: Int): P<Int, Int> = P { lt(n) or eq(n) }

class FailTester(private val error: ParseFail, private val stream: TextStream) {
    var message: String = error.error
        set(m) { if (error.error != m) println("Error ('${error.error}') != '$m'") }

    var position: Position = error.position
        set(p) { if (error.position != p) println("Error position ('${error.position}') != '$p'") }
}

class ParseTester<T>(name: String, parser: P<Int, T>, private vararg val numbers: Int): Tester(name) {
    private val result: ParseResult<Int, T>
    private val stream = TextStream.create(numbers.joinToString(" "))

    init {
        val state = ListParserState.new(numbers.fold(listOf<PositionedValue<Int>>()) { ns, n ->
            val p = ns.lastOrNull()?.position?.after(2) ?: Position(0)
            ns + listOf(n positioned p.extend(n.toString().length))
        }, stream)

        result = parser.parse(state)
    }

    fun success(fn: Tester.(T, Int?, Position) -> Unit) = when (result) {
        is ParseSuccess -> fn(this, result.value, result.nextState.token, result.nextState.position)
        is ParseFail -> error("Parse of $numbers failed: ${result.formatError(stream)}")
    }

    fun success(value: T) = success { v, _, _ ->
        assertValueEquals(value, v)
    }

    fun success(value: T, nextStateToken: Int?, nextStatePosition: Position) = success { v, t, p ->
        assertValueEquals(value, v)
        assertValueEquals(nextStateToken, t)
        assertValueEquals(nextStatePosition, p)
    }

    fun fail(fn: Tester.(String, Position, List<ParseFail>) -> Unit) = when (result) {
        is ParseSuccess -> error("Parse was successful (${result.value})")
        is ParseFail -> fn(this, result.error, result.position, result.causes)
    }

    fun fail(message: String, position: Position) = fail { m, p, _ ->
        assertValueEquals(message, m)
        assertValueEquals(position, p)
    }
}

fun <T> Tester.parseTest(name: String, parser: P<Int, T>, vararg numbers: Int, fn: ParseTester<T>.() -> Unit) {
    child(ParseTester(name, parser, *numbers), fn)
}
