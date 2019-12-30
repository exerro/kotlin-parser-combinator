package astify_tests

import astify.Position
import astify.util.PositionedValue
import astify.P
import test

fun parserTests() = test("ASTify Parsing") {
    parseTest("nothing", iP { nothing }) {
        success(
            value = Unit,
            nextStateToken = null,
            nextStatePosition = Position.start
        )
    }

    parseTest("anything", iP { anything }, 12, 2, 3) {
        success(
            value = 12,
            nextStateToken = 2,
            nextStatePosition = Position(3)
        )
    }

    parseTest("anything fail", iP { anything }) {
        fail(
            message = "Expected any token",
            position = Position.start
        )
    }

    parseTest("eof", iP { eof }) {
        success(
            value = Unit,
            nextStateToken = null,
            nextStatePosition = Position.start
        )
    }

    parseTest("eof fail", iP { eof }, 12) {
        fail(
            message = "Expected EOF",
            position = Position(0, 1)
        )
    }

    parseTest("newline", iP { anything and newline }, 10, 12) {
        fail(
            message = "Expected token on next line",
            position = Position(3, 4)
        )
    }

    parseTest("sameLine", iP { anything and sameLine }, 10, 12) {
        success(
            value = 10 to Unit,
            nextStateToken = 12,
            nextStatePosition = Position(3, 4)
        )
    }

    parseTest("value", iP { value { 3 } }, 15) {
        success(
            value = 3,
            nextStateToken = 15,
            nextStatePosition = Position(0, 1)
        )
    }

    // TODO: satisfying
    // TODO: convertType

    parseTest("sequence", P.seq<Int, Int> {
        val (a) = anything
        val (b) = anything
        a + b
    }, 1, 2) {
        success(
            value = 3,
            nextStateToken = null,
            nextStatePosition = Position(3)
        )
    }

    parseTest("sequence if (match)", P.seq<Int, Int> {
        if (parse(anything)) {
            val (b) = anything
            b
        } else {
            0
        }
    }, 1, 2) {
        success(2)
    }

    parseTest("sequence if (no match)", P.seq<Int, Int> {
        if (parse(anything)) {
            val (b) = anything
            b
        } else {
            0
        }
    }) {
        success(0)
    }

    parseTest("sequence fail", P.seq<Int, Int> {
        val (a) = anything
        val (b) = anything
        a + b
    }, 1) {
        fail(
            message = "Expected any token",
            position = Position(1)
        )
    }

    parseTest("map", iP { anything map { it + 1 } }, 12) {
        success(13)
    }

    parseTest("bind", iP { anything bind { x -> anything map { x to it } } }, 1, 2, 3) {
        success(
            value = 1 to 2,
            nextStateToken = 3,
            nextStatePosition = Position(4)
        )
    }

    parseTest("lazy", iP { lazy { anything } }, 12, 2) {
        success(
            value = 12,
            nextStateToken = 2,
            nextStatePosition = Position(3)
        )
    }

    parseTest("positioned", iP { positioned(anything) }, 356, 3) {
        success(
            value = PositionedValue(356, Position(0, 2)),
            nextStateToken = 3,
            nextStatePosition = Position(4)
        )
    }

    parseTest("positioned 2", iP { anything keepRight positioned(anything) }, 356, 3) {
        success(
            value = PositionedValue(3, Position(4)),
            nextStateToken = null,
            nextStatePosition = Position(5)
        )
    }

    parseTest("optional (match)", iP { optional(anything) }, 50) {
        success(50)
    }

    parseTest("optional (no match)", iP { optional(anything) }) {
        success(null)
    }

    parseTest("many (many)", iP { many(anything) }, 1, 2, 3) {
        success(
            value = listOf(1, 2, 3),
            nextStateToken = null,
            nextStatePosition = Position(5)
        )
    }

    parseTest("many (none)", iP { many(anything) }) {
        success(
            value = listOf(),
            nextStateToken = null,
            nextStatePosition = Position.start
        )
    }

    parseTest("many (lower count)", iP { many(1, anything) }, 23) {
        success(
            value = listOf(23),
            nextStateToken = null,
            nextStatePosition = Position(2)
        )
    }

    parseTest("many (lower count fail)", iP { many(3, anything) }, 42) {
        fail(
            message = "Expected any token",
            position = Position(2)
        )
    }

    // TODO: oneOf
    // TODO: branch
    // TODO: satisfying

    parseTest("and", iP { anything and anything }, 1, 2, 3) {
        success(
            value = 1 to 2,
            nextStateToken = 3,
            nextStatePosition = Position(4)
        )
    }

    parseTest("enables (match)", iP { anything enables anything }, 1, 2, 3) {
        success(
            value = 2,
            nextStateToken = 3,
            nextStatePosition = Position(4)
        )
    }

    parseTest("enables (no match)", iP { anything enables anything }) {
        success(
            value = null,
            nextStateToken = null,
            nextStatePosition = Position.start
        )
    }

    parseTest("enables (match fail)", iP { anything enables anything }, 3) {
        fail(
            message = "Expected any token",
            position = Position(1)
        )
    }

    parseTest("keepLeft", iP { anything keepLeft anything }, 1, 2, 3) {
        success(
            value = 1,
            nextStateToken = 3,
            nextStatePosition = Position(4)
        )
    }

    parseTest("keepRight", iP { anything keepRight anything }, 1, 2, 3) {
        success(
            value = 2,
            nextStateToken = 3,
            nextStatePosition = Position(4)
        )
    }

    // TODO: sepBy
    // TODO: sepBy0
    // TODO: orEither
    // TODO: or
    // TODO: orElse
    // TODO: until
    // TODO: wrap
}
