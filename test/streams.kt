import kotlin.test.assertEquals

fun textStreamTests() {
    test("TextStream") { it
            .value(StringTextStream("Hello"), "string text stream") { stream -> stream
                    .assertEquals(stream.value.peekNextChar(), 'H')
                    .assertEquals(stream.value.isEOF(), false)
                    .assertEquals(stream.value.readNextChar(), 'H')
                    .assertEquals(stream.value.peekNextChar(), 'e')
                    .assertEquals(stream.value.readNextChar(), 'e')
                    .assertEquals(stream.value.readNextChar(), 'l')
                    .assertEquals(stream.value.readNextChar(), 'l')
                    .assertEquals(stream.value.readNextChar(), 'o')
                    .assertEquals(stream.value.isEOF(), true)
            }
            .value(FileTextStream("test/file-text-stream.txt"), "file text stream") { stream -> stream
                    .assertEquals(stream.value.peekNextChar(), 'H')
                    .assertEquals(stream.value.isEOF(), false)
                    .assertEquals(stream.value.readNextChar(), 'H')
                    .assertEquals(stream.value.peekNextChar(), 'e')
                    .assertEquals(stream.value.readNextChar(), 'e')
                    .assertEquals(stream.value.readNextChar(), 'l')
                    .assertEquals(stream.value.readNextChar(), 'l')
                    .assertEquals(stream.value.readNextChar(), 'o')
                    .assertEquals(stream.value.isEOF(), true)
            }
    }

    assertEquals(1, 1)
}
