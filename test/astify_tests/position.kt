package astify_tests

import assert
import assertValueEquals
import assertValueNotEquals
import astify.Position
import astify.TextStream
import child
import test

fun positions() = test("ASTify Position") {
    val p1 = Position(0)
    val p2 = Position(4, 5)
    val p3 = Position(3, 6)
    val p4 = Position(7)

    child("equality") {
        assertValueEquals(p1.first, 0)
        assertValueEquals(p1.second, 0)
        assertValueEquals(p1, p1)
        assertValueEquals(p3.first, 3)
        assertValueEquals(p3.second, 6)
        assertValueEquals(p3, p3)
        assertValueNotEquals(p1, p3)
    }

    child("to") {
        assertValueEquals(p1 to p2, Position(0, 5))
        assertValueEquals(p2 to p3, Position(4, 6))
    }

    child("extend") {
        assertValueEquals(p1 extend 3, Position(0, 2))
        assertValueEquals(p3 extend 2, Position(3, 4))
    }

    child("after") {
        assertValueEquals(p2 after 1, Position(6, 6))
        assertValueEquals(p3 after 2, Position(8, 8))
    }

    child("follows") {
        assert(p4 follows p3, "%~$p4%- does not follow %~$p3%-", "%~$p4%- follows %~$p3%-")
        assert(!(p4 follows p2), "%~$p4%- follows %~$p2%-", "%~$p4%- does not follow %~$p2%-")
    }

    child("line/column numbers") {
        val str = TextStream.create("abc\ndef\nghi\njkl")
        assertValueEquals(p1.columnNumber1(str), 1)
        assertValueEquals(p2.columnNumber1(str), 1)
        assertValueEquals(p3.columnNumber2(str), 3)
        assertValueEquals(p1.lineNumber1(str), 1)
        assertValueEquals(p3.lineNumber2(str), 2)
        assertValueEquals(p4.colNumbers(str), 4 to 4)
        assertValueEquals(p4.lineNumbers(str), 2 to 2)
    }

    child("line contents") {
        val str = TextStream.create("abc\ndef\nghi\njkl")
        assertValueEquals(p3.line1(str), "abc")
        assertValueEquals(p3.line2(str), "def")
        assertValueEquals(p3.lines(str), "abc" to "def")
    }

    child("summary") {
        val str = TextStream.create("abc\ndef\nghi\njkl")
        assertValueEquals(p1.summary(str), "[line 1 col 1]")
        assertValueEquals(p2.summary(str), "[line 2 col 1 .. 2]")
        assertValueEquals(p3.summary(str), "[line 1 col 4 .. line 2 col 3]")
    }

    child("line pointer") {
        val str = TextStream.create("abc\ndef\nghi\njkl")
        val p1p = p1.linePointer(str)
        val p2p = p2.linePointer(str)
        val p3p = p3.linePointer(str)

        assert(p1p == "1: abc\n   ^",
                "incorrect p1 line pointer: '${p1p.stripNewlines()}'",
                "correct p1 line pointer")

        assert(p2p == "2: def\n   ^^",
                "incorrect p2 line pointer: '${p2p.stripNewlines()}'",
                "correct p2 line pointer")

        assert(p3p == " 1: abc\n       ^ ...\n 2: def\n... ^^^",
                "incorrect p3 line pointer: '${p3p.stripNewlines()}'",
                "correct p3 line pointer")
    }

    test("toString()") {
        assertValueEquals(p1.toString(), "Position(0 .. 0)")
        assertValueEquals(p3.toString(), "Position(3 .. 6)")
    }

    test("start") {
        assertValueEquals(Position.start, Position(0, 0))
    }
}

private fun String.stripNewlines() = replace("\n", "\\n")
