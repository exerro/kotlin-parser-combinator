
fun textStreamTests() {
    test("TextStream") {
        value(StringTextStream("Hello"), "string") {
            value(value.peekNextChar(), "$name.peekNextChar()") { assertEquals('H') }
            value(value.isEOF(), "$name.isEOF()") { assertEquals(false) }
            value(value.readNextChar(), "$name.readNextChar()") { assertEquals('H') }
            value(value.peekNextChar(), "$name.peekNextChar()") { assertEquals('e') }
            value(value.readNextChar(), "$name.readNextChar()") { assertEquals('e') }
            value(value.readNextChar(), "$name.readNextChar()") { assertEquals('l') }
            value(value.readNextChar(), "$name.readNextChar()") { assertEquals('l') }
            value(value.readNextChar(), "$name.readNextChar()") { assertEquals('o') }
            value(value.isEOF(), "$name.isEOF()") { assertEquals(true) }
        }

        value(FileTextStream("test/file-text-stream.txt"), "file") {
            value(value.peekNextChar(), "$name.peekNextChar()") { assertEquals('H') }
            value(value.isEOF(), "$name.isEOF()") { assertEquals(false) }
            value(value.readNextChar(), "$name.readNextChar()") { assertEquals('H') }
            value(value.peekNextChar(), "$name.peekNextChar()") { assertEquals('e') }
            value(value.readNextChar(), "$name.readNextChar()") { assertEquals('e') }
            value(value.readNextChar(), "$name.readNextChar()") { assertEquals('l') }
            value(value.readNextChar(), "$name.readNextChar()") { assertEquals('l') }
            value(value.readNextChar(), "$name.readNextChar()") { assertEquals('o') }
            value(value.isEOF(), "$name.isEOF()") { assertEquals(true) }
        }
    }
}
