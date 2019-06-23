
val p = parser { branch(
        identifier to integer,
        integer to identifier
) }

fun parsingTests() {
    println(p parse "+")

    test("Parsing") {
        // TODO!
    }
}
