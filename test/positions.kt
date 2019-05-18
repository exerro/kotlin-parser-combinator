import kotlin.test.assertEquals

fun positionTests() {
    test("Position") { it
            .value(Position(1, 5, 1, 7)) { pos -> pos
                    .hasValue(Position(1, 5, 1, 7))
                    .assertEquals(pos.value.line1, 1)
                    .assertEquals(pos.value.char1, 5)
                    .assertEquals(pos.value.line2, 1)
                    .assertEquals(pos.value.char2, 7)
            }
            .value(Position(1, 5, 1, 7).to(Position(2, 4, 5, 6)), "to()") { pos -> pos
                    .hasValue(Position(1, 5, 5, 6))
            }
            .value(Position(1, 5, 1, 7).extendTo(3), "extendTo()") { pos -> pos
                    .hasValue(Position(1, 5, 1, 7))
            }
            .value(Position(2, 3, 4, 5).after(6), "after()") { pos -> pos
                    .hasValue(Position(4, 11, 4, 11))
            }
            .child("getPositionString()") { str -> str
                    .assertEquals(Position(1, 1, 1, 1).getPositionString(), "[line 1 col 1]")
                    .assertEquals(Position(1, 1, 1, 5).getPositionString(), "[line 1 col 1 .. 5]")
                    .assertEquals(Position(1, 1, 2, 4).getPositionString(), "[line 1 col 1 .. line 2 col 4]")
            }
            .child("getErrorString()") { str -> str
                    .assertEquals(Position(1, 1, 1, 1).getErrorString("oh noes", getStream()), "oh noes\n    Hello world\n    ^")
                    .assertEquals(Position(1, 1, 1, 5).getErrorString("oh noes", getStream()), "oh noes\n    Hello world\n    ^^^^^")
                    .assertEquals(Position(1, 1, 2, 4).getErrorString("oh noes", getStream()), "oh noes\n    Hello world\n    ^^^^^^^^^^^...\n    here\n... ^^^^")
            }
    }

    assertEquals(1, 1)
}

private fun getStream(): TextStream
    = StringTextStream("Hello world\nhere")
