package astify.monadic.util

import astify.monadic.*
import astify.util.PositionedValue
import astify.util.positioned

/** Consume a token and yield that as its result. Fails if no token was
 *  available to consume. */
val <State: PositionedTokenParserState<State, Token, Position>, Token, Position>
        Parsing<State, String>.anythingPositioned
        : P<State, String, Pair<Token, Position>>
    get() = P { s -> when (val t = s.token) {
        null -> ParseResult.Failure("expected token, got EOF", s)
        else -> ParseResult.Success(t to s.position, s.next)
    } }

fun <Value> Parsing<TextStreamParserState, String>.positioned(
        p: P<TextStreamParserState, String, Value>
): P<TextStreamParserState, String, PositionedValue<Value>> =
        p { nothing flatMap { _, s0 -> p flatMap { v, s1 ->
            P.pure<TextStreamParserState, PositionedValue<Value>>(
                    v positioned (s0.position to s1.lastPosition)
            ) } } }
