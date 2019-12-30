
//fun positionTests() {
//    test("Position") {
//        value(Position(1, 5, 1, 7), "position") {
//            assertEquals(Position(1, 5, 1, 7))
//            assertPropertyEquals(Position::line1, 1)
//            assertPropertyEquals(Position::char1, 5)
//            assertPropertyEquals(Position::line2, 1)
//            assertPropertyEquals(Position::char2, 7)
//        }
//
//        value(Position(1, 5, 1, 7) to Position(2, 4, 5, 6), "to()") {
//            assertEquals(Position(1, 5, 5, 6))
//        }
//
//        value(Position(1, 5, 1, 7) extendTo 3, "extendTo()") {
//            assertEquals(Position(1, 5, 1, 7))
//        }
//
//        value(Position(2, 3, 4, 5) after 6, "after()") {
//            assertEquals(Position(4, 11, 4, 11))
//        }
//    }
//
//    test("Position.getPositionString()") {
//        value(Position(1, 1, 1, 1).getPositionString(), "char") { assertEquals("[line 1 col 1]") }
//        value(Position(1, 1, 1, 5).getPositionString(), "line") { assertEquals("[line 1 col 1 .. 5]") }
//        value(Position(1, 1, 2, 4).getPositionString(), "multiline") { assertEquals("[line 1 col 1 .. line 2 col 4]") }
//    }
//
//    test("Position.getErrorString()") {
//        value(Position(1, 1, 1, 1).getErrorString("oh noes", getStream()), "char") { assertEquals("oh noes\n    Hello world\n    ^") }
//        value(Position(1, 1, 1, 5).getErrorString("oh noes", getStream()), "line") { assertEquals("oh noes\n    Hello world\n    ^^^^^") }
//        value(Position(1, 1, 2, 4).getErrorString("oh noes", getStream()), "multiline") { assertEquals("oh noes\n    Hello world\n    ^^^^^^^^^^^...\n    here\n... ^^^^") }
//    }
//}
//
//private fun getStream(): TextStream
//    = StringTextStream("Hello world\nhere")
