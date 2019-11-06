
val p1 = parser { branch(
        identifier to (identifier then integer),
        integer to (integer then identifier)
) }

val p2 = parser { oneOf(
        identifier then integer,
        integer then identifier
) }

fun parsingTests() {
//    println(p1 parse "hello world")
    println(p2 parse "hello world")

    test("Parsing") {
        // TODO!
    }
}
