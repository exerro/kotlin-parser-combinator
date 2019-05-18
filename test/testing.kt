
private val testers = ArrayList<Tester>()

fun test(name: String, test: (Tester) -> Unit) {
    val tester = Tester(name)
    testers.add(tester)
    test(tester)
}

fun printTestResults() {
    for (tester in testers) {
        println("Test '\u001B[33m${tester.name}\u001B[0m'\n\t${tester.buffer.toString().replace("\n", "\n\t")}")
        println()
    }
}

open class Tester(val name: String) {
    internal val buffer = StringBuilder()
}

class ValueTester<T>(val value: T, name: String): Tester(name)

private fun fmt(text: String, bold: Boolean): String {
    val bs = if (bold) ";1" else ""
    return text
            .replace("%r", "\u001B[31${bs}m")
            .replace("%g", "\u001B[32${bs}m")
            .replace("%y", "\u001B[33${bs}m")
            .replace("%b", "\u001B[34${bs}m")
            .replace("%m", "\u001B[35${bs}m")
            .replace("%c", "\u001B[36${bs}m")
            .replace("%w", "\u001B[37${bs}m")
            .replace("%-", "\u001B[0m")
            .replace("%R", "\u001B[31;1m")
            .replace("%G", "\u001B[32;1m")
            .replace("%Y", "\u001B[33;1m")
            .replace("%B", "\u001B[34;1m")
            .replace("%M", "\u001B[35;1m")
            .replace("%C", "\u001B[36;1m")
            .replace("%W", "\u001B[37;1m")
            .replace("%~", "\u001B[0;1m")
}

private fun fg(col: Colour, bold: Boolean): String {
    val bs = if (bold) ";1" else ""
    return when (col) {
        Colour.NONE -> ""
        Colour.RED -> "\u001B[31${bs}m"
        Colour.GREEN -> "\u001B[32${bs}m"
        Colour.YELLOW -> "\u001B[33${bs}m"
        Colour.BLUE -> "\u001B[34${bs}m"
        Colour.MAGENTA -> "\u001B[35${bs}m"
        Colour.CYAN -> "\u001B[36${bs}m"
        Colour.WHITE -> "\u001B[37${bs}m"
    }
}

enum class Colour {
    NONE,
    RED,
    GREEN,
    YELLOW,
    BLUE,
    MAGENTA,
    CYAN,
    WHITE
}

fun <T: Tester> T.write(text: String, foreground: Colour = Colour.NONE, bold: Boolean = false): T {
    buffer.append("${fg(foreground, bold)}${fmt(text, bold)}\u001b[0m")
    return this
}

fun <T: Tester> T.writeln(text: String, foreground: Colour = Colour.NONE, bold: Boolean = false): T {
    write(text, foreground, bold);
    write("\n")
    return this
}

fun <T: Tester> T.error(text: String): T
        = writeln("ERROR: $text", Colour.RED, true)

fun <T: Tester> T.assert(condition: Boolean, error: String, message: String? = null): T
        = if (condition) if (message != null) { write(message); this } else { this } else { error(error) }

fun <A, B, T: Tester> T.assertEquals(a: A, b: B): T
        = if (a == b) { writeln("%~$a%- %w==%- %~$b%-") } else { error("$a != $b") }

fun <A, B, T: Tester> T.assertNotEquals(a: A, b: B): T
        = if (a != b) { writeln("%~$a%- %w!=%- %~$b%-") } else { error("$a == $b") }

fun <T: Tester> T.child(name: String, test: (Tester) -> Unit): T
        = child(Tester(name), test)

fun <T: Tester, V> T.value(value: V, name: String = value.toString(), test: (ValueTester<V>) -> Unit): T
        = child(ValueTester(value, name), test)

fun <V, T: ValueTester<V>> T.hasValue(value: V): T
    = assertEquals(this.value, value)

fun <V, T: ValueTester<V>> T.notHasValue(value: V): T
        = assertNotEquals(this.value, value)

private fun <T: Tester, R: Tester> T.child(tester: R, test: (R) -> Unit): T {
    test(tester)
    writeln("Test '%y${tester.name}%-'\n\t" + tester.buffer.toString().replace("\n", "\n\t"))
    return this
}
