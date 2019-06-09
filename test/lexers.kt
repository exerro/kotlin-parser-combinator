
fun lexerTests() {
    test("Lexer") { it
            .lexer("hello + 0.0 1.0 0 key", LexerTools.keywords(listOf("key")) lexUnion LexerTools.floats() lexUnion LexerTools.any()) { lexer -> lexer
                    .printToken()
                    .assertTokenHasType(TOKEN_IDENT)
                    .assertTokenEquals("hello")
                    .assertTokenHasPosition(Position(1, 1, 1, 5))
                    .nextToken()
                    .printToken()
                    .assertTokenHasType(TOKEN_SYM)
                    .assertTokenEquals("+")
                    .assertTokenHasPosition(Position(1, 7))
                    .nextToken()
                    .printToken()
                    .assertTokenHasType(TOKEN_FLOAT)
                    .assertTokenEquals("0.0")
                    .assertTokenHasPosition(Position(1, 9, 1, 11))
                    .nextToken()
                    .printToken()
                    .assertTokenHasType(TOKEN_FLOAT)
                    .assertTokenEquals("1.0")
                    .assertTokenHasPosition(Position(1, 13, 1, 15))
                    .nextToken()
                    .printToken()
                    .assertTokenHasType(TOKEN_INT)
                    .assertTokenEquals("0")
                    .assertTokenHasPosition(Position(1, 17))
                    .nextToken()
                    .printToken()
                    .assertTokenHasType(TOKEN_KEYWORD)
                    .assertTokenEquals("key")
                    .assertTokenHasPosition(Position(1, 19, 1, 21))
            }
            .lexer("hello world 312 word", LexerTools.identifiers() lexUnion LexerTools.integers()) { lexer -> lexer
                    .printToken()
                    .nextToken()
                    .assertTokenHasType(TOKEN_IDENT)
                    .assertTokenEquals("world")
                    .assertTokenHasPosition(Position(1, 7, 1, 11))
                    .subNextToken { sub -> sub
                            .assertTokenHasType(TOKEN_INT)
                            .assertTokenEquals("312")
                            .assertTokenHasPosition(Position(1, 13, 1, 15))
                            .subNextToken { sub2 -> sub2
                                    .assertTokenHasType(TOKEN_IDENT)
                                    .assertTokenEquals("word")
                                    .assertTokenHasPosition(Position(1, 17, 1, 20))
                            }
                    }
                    .assertTokenHasType(TOKEN_IDENT)
                    .assertTokenEquals("world")
                    .assertTokenHasPosition(Position(1, 7, 1, 11))
            }
            .lexer("single-token second-token", LexerTools.matching({ it in 'a' .. 'z' }, { it in 'a' .. 'z' || it == '-' }) { str, pos ->
                Token(TOKEN_IDENT, str, pos extendTo str.length)
            }) { lexer -> lexer
                    .printToken()
                    .assertTokenHasType(TOKEN_IDENT)
                    .assertTokenEquals("single-token")
                    .assertTokenHasPosition(Position(1, 1, 1, 12))
                    .nextToken()
                    .printToken()
                    .assertTokenHasType(TOKEN_IDENT)
                    .assertTokenEquals("second-token")
                    .assertTokenHasPosition(Position(1, 14, 1, 25))
            }
            .lexer("'a' '\\b' \"hello\" '\"hello\" \\'world\\'' \"\n\"", LexerTools.strings(true)) { lexer -> lexer
                    .printToken()
                    .assertTokenHasType(TOKEN_CHAR)
                    .assertTokenEquals("'a'")
                    .assertTokenHasPosition(Position(1, 1, 1, 3))
                    .nextToken()
                    .printToken()
                    .assertTokenHasType(TOKEN_CHAR)
                    .assertTokenEquals("'\\b'")
                    .assertTokenHasPosition(Position(1, 5, 1, 8))
                    .nextToken()
                    .printToken()
                    .assertTokenHasType(TOKEN_STR)
                    .assertTokenEquals("\"hello\"")
                    .assertTokenHasPosition(Position(1, 10, 1, 16))
                    .nextToken()
                    .printToken()
                    .assertTokenHasType(TOKEN_STR)
                    .assertTokenEquals("'\"hello\" \\'world\\''")
                    .assertTokenHasPosition(Position(1, 18, 1, 36))
                    .nextToken()
                    .printToken()
                    .assertTokenHasType(TOKEN_STR)
                    .assertTokenEquals("\"\n\"")
                    .assertTokenHasPosition(Position(1, 38, 2, 1))
            }
            .lexer("'a' '\\b'", LexerTools.chars()) { lexer -> lexer
                    .printToken()
                    .assertTokenHasType(TOKEN_CHAR)
                    .assertTokenEquals("'a'")
                    .assertTokenHasPosition(Position(1, 1, 1, 3))
                    .nextToken()
                    .printToken()
                    .assertTokenHasType(TOKEN_CHAR)
                    .assertTokenEquals("'\\b'")
                    .assertTokenHasPosition(Position(1, 5, 1, 8))
            }
            .lexer("a\n b", LexerTools.identifiers()) { lexer -> lexer
                    .printToken()
                    .assertTokenHasType(TOKEN_IDENT)
                    .assertTokenEquals("a")
                    .assertTokenHasPosition(Position(1, 1))
                    .nextToken()
                    .printToken()
                    .assertTokenHasType(TOKEN_IDENT)
                    .assertTokenEquals("b")
                    .assertTokenHasPosition(Position(2, 2))
            }
    }
}

class LexerTester(
        name: String,
        lexer: Lexer,
        private val parent: LexerTester?
): ValueTester<Lexer>(lexer, name) {
    private val lastToken: Token = try { lexer.next().first } catch(e: TokenParseException) {
        (parent ?: this).error(e.message.toString())
        Token(TOKEN_EOF, "<ERROR>", Position(0, 0))
    }

    fun nextToken(): LexerTester {
        return try {
            LexerTester(name, value.next().second, parent ?: this)
        }
        catch (e: TokenParseException) {
            LexerTester(name, this.value, parent ?: this)
        }
    }
    fun subNextToken(test: (LexerTester) -> Unit): LexerTester { (parent ?: this).child(LexerTester("$name (sub)", value.next().second, null), test); return this }
    fun assertTokenEquals(value: String): LexerTester { (parent ?: this).assertEquals(lastToken.text, value); return this }
    fun assertTokenHasType(type: TokenType): LexerTester { (parent ?: this).assertEquals(lastToken.type, type); return this }
    fun assertTokenHasPosition(position: Position): LexerTester { (parent ?: this).assertEquals(lastToken.getPosition(), position); return this }
    fun printToken(): LexerTester { (parent ?: this).writeln(lastToken.toString(), Colour.BLUE); return this }
}

fun <T: Tester> T.lexer(text: String, consume: (TextStream, Position) -> Token?, test: (LexerTester) -> Unit): T {
    val lexer = Lexer(StringTextStream(text), consume)
    return child(LexerTester("Lexer for '${text.replace("\n", "\\n")}'", lexer, null), test)
}
