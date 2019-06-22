
fun positionedTests() {
    test("Positioned") {
        value(Token(TOKEN_EOF, "Hello", Position(1, 2, 3, 4)), "token") {
            value(value.getValue(), "$name.getValue()") { assertEquals("Hello") }
            value(value.getPosition(), "$name.getPosition()") { assertEquals(Position(1, 2, 3, 4)) }
            value(value.toString(), "$name.toString()") { assertEquals("'Hello' @ [line 1 col 2 .. line 3 col 4]") }

            value(value.withValue(5), "$name.withValue(5)") {
                assertValueEquals(value.getValue(), 5)
                assertValueEquals(value.getPosition(), Position(1, 2, 3, 4))
            }
            value(value.mapValue { "$it world" }, "$name.mapValue(+\" world\")") {
                assertValueEquals(value.getValue(), "Hello world")
                assertValueEquals(value.getPosition(), Position(1, 2, 3, 4))
            }
            value(value.withPosition(Position(5, 6, 7, 8)), "$name.withPosition()") {
                assertValueEquals(value.getValue(), "Hello")
                assertValueEquals(value.getPosition(), Position(5, 6, 7, 8))
            }
        }
    }
}
