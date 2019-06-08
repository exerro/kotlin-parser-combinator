fun parsingTests() {
    test("Parsing") { p -> p
            .parser("Hello world 50") { parser -> parser
                    .on("just identifier", ParseTools.token(TOKEN_IDENT)) { results ->
                        results
                                .assertSuccess()
                                .assertErrorCount(0)
                                .printResults()
                                .onSuccess { value -> value
                                        .assertEquals(value.value.type, TOKEN_IDENT)
                                        .assertEquals(value.value.text, "Hello")
                                }
                    }
                    .on("two identifiers",
                            ParseTools.token<Unit>(TOKEN_IDENT).bindIn { first -> ParseTools.token<Unit>(TOKEN_IDENT) map { Pair(first, it) } }
                    ) { results -> results
                            .assertSuccess()
                            .assertErrorCount(0)
                            .printResults()
                            .onSuccess { value -> value
                                    .assertEquals(value.value.first.type, TOKEN_IDENT)
                                    .assertEquals(value.value.first.text, "Hello")
                                    .assertEquals(value.value.second.type, TOKEN_IDENT)
                                    .assertEquals(value.value.second.text, "world")
                            }
                    }
                    .on("list of identifiers", ParseTools.list(ParseTools.token<Unit>(TOKEN_IDENT)).single().filterErrors(true)) { results -> results
                            .assertSuccess()
                            .assertErrorCount(0)
                            .printResults()
                            .onSuccess { value -> value
                                    .assertEquals(value.value.size, 2)
                                    .assertEquals(value.value[0].type, TOKEN_IDENT)
                                    .assertEquals(value.value[0].text, "Hello")
                                    .assertEquals(value.value[1].type, TOKEN_IDENT)
                                    .assertEquals(value.value[1].text, "world")
                            }
                    }
                    .on("list of identifiers then integer",
                            (ParseTools.list(ParseTools.token<Unit>(TOKEN_IDENT)).single() bindIn { first -> ParseTools.token<Unit>(TOKEN_INT) map { Pair(first, it) } }).filterErrors(true)
                    ) { results -> results
                            .assertSuccess()
                            .assertErrorCount(0)
                            .printResults()
                            .onSuccess { value -> value
                                    .assertEquals(value.value.first.size, 2)
                                    .assertEquals(value.value.first[0].type, TOKEN_IDENT)
                                    .assertEquals(value.value.first[0].text, "Hello")
                                    .assertEquals(value.value.first[1].type, TOKEN_IDENT)
                                    .assertEquals(value.value.first[1].text, "world")
                                    .assertEquals(value.value.second.type, TOKEN_INT)
                                    .assertEquals(value.value.second.text, "50")
                            }
                    }
                    .on("list of identifiers then identifier",
                            (ParseTools.list(ParseTools.token<Unit>(TOKEN_IDENT))
                                    followedBy ParseTools.token(TOKEN_IDENT)).single().filterErrors(true)
                    ) { results -> results
                            .assertSuccess()
                            .assertErrorCount(0)
                            .printResults()
                    }
                    .on("list of identifiers then string",
                            (ParseTools.list(ParseTools.token<Unit>(TOKEN_IDENT))
                                    followedBy ParseTools.token(TOKEN_STR)).filterErrors(true)
                    ) { results -> results
                            .assertFailure()
                            .assertErrorCount(2)
                            .shouldError("Expected any string, got integer '50'", Position(1, 13, 1, 14))
                            .shouldError("Expected any identifier, got integer '50'", Position(1, 13, 1, 14))
                    }
            }
            .parser("a b c d e f g") { parser -> parser
                    .on("list list", (ParseTools.list(ParseTools.token<Unit>(TOKEN_IDENT)) followedBy ParseTools.list(ParseTools.token(TOKEN_IDENT))).single()) { results -> results
                            .assertSuccess()
                            .onSuccess { value -> value
                                    .assertEquals(value.value.size, 7)
                            }
                    }
                    .on("empty list",
                            (ParseTools.list(ParseTools.token<Unit>(TOKEN_STR))).single()
                    ) { results -> results
                            .assertSuccess()
                            .onSuccess { value -> value
                                    .assertEquals(value.value.size, 0)
                            }
                    }
                    .on("list of pairs",
                            (ParseTools.list(ParseTools.token<Unit>(TOKEN_IDENT) followedBy ParseTools.token(TOKEN_IDENT))).single()
                    ) { results -> results
                            .assertSuccess()
                            .onSuccess { value -> value
                                    .assertEquals(value.value.size, 3)
                            }
                    }
            }
            .parser(". b") { parser -> parser
                    .on("(ident|int) ident", (ParseTools.token<Unit>(TOKEN_IDENT) or ParseTools.token(TOKEN_INT) collectErrors "first token match failed") followedBy ParseTools.token(TOKEN_IDENT)) { results -> results
                            .assertErrorCount(1)
                            .shouldError("first token match failed")
                    }
            }
            .parserOn("regex 1", "ab*", regexRootExpr, ::regexLexer) { results -> results
                        .assertSuccess()
                        .printResults()
            }
            .parserOn("regex 2", "a|b|cd*", regexRootExpr, ::regexLexer) { results -> results
                    .assertSuccess()
                    .printResults()
            }
            .parserOn("regex 3", "^(a|b)*c*$", regexRootExpr, ::regexLexer) { results -> results
                    .assertSuccess()
                    .printResults()
            }
            .parserOn("regex 4", "^[ab]+$", regexRootExpr, ::regexLexer) { results -> results
                    .assertSuccess()
                    .printResults()
            }
            .parserOn("regex 5", "[a", regexRootExpr, ::regexLexer) { results -> results
                    .assertFailure()
                    .assertErrorCount(2)
                    .shouldError("Expected symbol ']', got EOF 'EOF'", Position(1, 3))
                    .shouldError("Expected any string, got EOF 'EOF'", Position(1, 3))
            }
            .parserOn("regex 6", "-", regexRootExpr, ::regexLexer) { results -> results
                    .assertFailure()
                    .assertErrorCount(1)
                    .shouldError("No viable alternatives", Position(1, 1))
            }
            .parserOn("regex 7", "a|", regexRootExpr.filterErrors(), ::regexLexer) { results -> results
                    .assertFailure()
                    .assertErrorCount(1)
                    .shouldError("No viable alternatives", Position(1, 3))
            }
            .parserOn("math 1", "1 + 2 * 3 ^ 4 * 5 - 6", mathParser.followedBy(ParseTools.token(TOKEN_EOF)).filterErrors(true)) { results -> results
                    .assertSuccess()
                    .assertErrorCount(0)
                    .onSuccess { value -> value
                            .assertEquals(value.value, 805)
                    }
            }
            .parserOn("math 1", "-2^4", mathParser.followedBy(ParseTools.token(TOKEN_EOF)).filterErrors(true)) { results -> results
                    .assertSuccess()
                    .assertErrorCount(0)
                    .onSuccess { value -> value
                            .assertEquals(value.value, -16)
                    }
            }
            .parserOn("math 1", "3 * -2 > ", mathParser.followedBy(ParseTools.token(TOKEN_EOF)).filterErrors(true)) { results -> results
                    .assertSuccess()
                    .assertErrorCount(0)
                    .onSuccess { value -> value
                            .assertEquals(value.value, -5)
                    }
            }
            .parserOn("math 1", "1 + 2 + 3", mathParser.followedBy(ParseTools.token(TOKEN_EOF)).filterErrors(true)) { results -> results
                    .assertSuccess()
                    .assertErrorCount(0)
                    .onSuccess { value -> value
                            .assertEquals(value.value, 6)
                    }
            }
    }
}

class ParseTester<U>(text: String, private val data: U): ValueTester<String>(
        text,
        "parser for '$text'"
)

class ParseResultTester<T, U>(
        values: ParseResultList<T, U>,
        name: String,
        val getStream: () -> TextStream
): ValueTester<ParseResultList<T, U>>(values, name)

fun <U> Tester.parser(text: String, data: U, test: (ParseTester<U>) -> Unit): Tester
        = child(ParseTester(text, data), test)

fun Tester.parser(text: String, test: (ParseTester<Unit>) -> Unit): Tester
        = child(ParseTester(text, Unit), test)

fun <T> Tester.parserOn(name: String, text: String, parser: Parser<T, Unit>, newl: (TextStream) -> Lexer = { Lexer(it, LexerTools.identifiers() lexUnion LexerTools.integers() lexUnion LexerTools.any()) }, test: (ParseResultTester<T, Unit>) -> Unit): Tester
        = child(ParseTester(text, Unit).on(name, parser, newl, test)) {}

fun <T, U> ParseTester<U>.on(name: String, parser: Parser<T, U>, newl: (TextStream) -> Lexer = { Lexer(it, LexerTools.identifiers() lexUnion LexerTools.integers() lexUnion LexerTools.any()) }, data: U, test: (ParseResultTester<T, U>) -> Unit): ParseTester<U> {
    val context = ParseContext(data, newl(StringTextStream(value)))
    return child(ParseResultTester(parser(context), name) { StringTextStream(value) }, test)
}

fun <T> ParseTester<Unit>.on(name: String, parser: Parser<T, Unit>, newl: (TextStream) -> Lexer = { Lexer(it, LexerTools.identifiers() lexUnion LexerTools.integers() lexUnion LexerTools.any()) }, test: (ParseResultTester<T, Unit>) -> Unit): ParseTester<Unit>
        = on(name, parser, newl, Unit, test)

fun <T, U> ParseResultTester<T, U>.assertSuccess(): ParseResultTester<T, U> {
    value .filter { it is ParseResult.ParseSuccess<T, U> } .ifEmpty {
        error("Parse didn't succeed")
        value.map { error((it as ParseResult.ParseFailure).error.getString(getStream)) }
    }
    return this
}

fun <T, U> ParseResultTester<T, U>.assertFailure(): ParseResultTester<T, U> {
    if (value.any { it is ParseResult.ParseSuccess<T, U> }) {
        error("Parse succeeded unexpectedly")
        value .filter { it is ParseResult.ParseSuccess } .map { error((it as ParseResult.ParseSuccess).value.toString()) }
    }
    else {
        writeln("Failed as expected")
    }
    return this
}

fun <T, U> ParseResultTester<T, U>.onSuccess(test: (ValueTester<T>) -> Unit): ParseResultTester<T, U> {
    value .filter { it is ParseResult.ParseSuccess } .mapIndexed { i, it -> it as ParseResult.ParseSuccess; child(ValueTester(it.value, "result ${i + 1} of $name"), test) }
    return this
}

fun <T, U> ParseResultTester<T, U>.printResults(): ParseResultTester<T, U> {
    value .filter { it is ParseResult.ParseSuccess } .map { writeln("%B${(it as ParseResult.ParseSuccess).value}%-") }
    return this
}

fun <T, U> ParseResultTester<T, U>.assertErrorCount(count: Int): ParseResultTester<T, U> {
    val ec = value .filter { it is ParseResult.ParseFailure } .size
    if (ec == count) {
        writeln("%B$count%b errors%- as expected")
    }
    else {
        error("$ec errors instead of $count")
    }
    return this
}

fun <T, U> ParseResultTester<T, U>.printErrors(): ParseResultTester<T, U> {
    value .filter { it is ParseResult.ParseFailure }.map { writeln("%r" + (it as ParseResult.ParseFailure).error.getString(getStream) + "%-") }
    return this
}

fun <T, U> ParseResultTester<T, U>.shouldError(message: String): ParseResultTester<T, U> {
    val errors = value .filter { it is ParseResult.ParseFailure && it.error.error == message } .map { it as ParseResult.ParseFailure }
    if (errors.isEmpty()) {
        error("No such error '$message'")
    }
    else {
        writeln("Errored '%b$message%-' as expected:%y\n\t" + errors.joinToString("\n\t") { it.error.getString(getStream).replace("\n", "\n\t") })
    }
    return this
}

fun <T, U> ParseResultTester<T, U>.shouldError(message: String, position: Position): ParseResultTester<T, U> {
    val errors = value .filter { it is ParseResult.ParseFailure && it.error.error == message && it.error.position == position } .map { it as ParseResult.ParseFailure }
    if (errors.isEmpty()) {
        error("No such error '$message'")
    }
    else {
        writeln("Errored '%b$message%-' as expected:%y\n\t" + errors.joinToString("\n\t") { it.error.getString(getStream).replace("\n", "\n\t") })
    }
    return this
}
