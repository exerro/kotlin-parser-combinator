
fun positionedTests() {
    test("Positioned") { it
            .value(Token(TOKEN_EOF, "Hello", Position(1, 2, 3, 4)), "token") { token -> token
                    .assertEquals(token.value.getValue(), "Hello")
                    .assertEquals(token.value.getPosition(), Position(1, 2, 3, 4))
                    .assertEquals(token.value.toString(), "'Hello' @ [line 1 col 2 .. line 3 col 4]")
                    .value(token.value.withValue(5), "withValue(5)") { n -> n
                            .assertEquals(n.value.getValue(), 5)
                            .assertEquals(n.value.getPosition(), Position(1, 2, 3, 4))
                    }
                    .value(token.value.mapValue { "$it world" }, "mapValue(+\" world\")") { n -> n
                            .assertEquals(n.value.getValue(), "Hello world")
                            .assertEquals(n.value.getPosition(), Position(1, 2, 3, 4))
                    }
                    .value(token.value.withPosition(Position(5, 6, 7, 8)), "withPosition()") { n -> n
                            .assertEquals(n.value.getValue(), "Hello")
                            .assertEquals(n.value.getPosition(), Position(5, 6, 7, 8))
                    }
            }
    }
}
