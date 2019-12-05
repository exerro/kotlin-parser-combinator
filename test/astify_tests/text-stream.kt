package astify_tests

import assertValueEquals
import assertValueNotEquals
import astify.TextStream
import test

fun textStream() = test("ASTify TextStream") {
    val stream = TextStream("abc")

    assertValueEquals(stream.raw, "abc")
    assertValueEquals(stream.char, 'a')
    assertValueNotEquals(stream.next, null)
    assertValueEquals(stream.next?.char, 'b')
    assertValueEquals(stream.next?.next?.char, 'c')
    assertValueEquals(stream.next?.next?.next, null)
}
