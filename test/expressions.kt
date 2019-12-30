
//fun parsingExpressionTests() {
//    test("Parsing expressions") {
//        // TODO!
//    }
//}
//
//val termParser = parser { integer map { it.text.toInt() } }
//val mathParser = expressionParser<Int, Token>(termParser) {
//    defaultConverter()
//    unaryl("unm", "-", 6)
//    unaryl("lop", "<", 2)
//    unaryr("rop", ">", 2)
//    infixl("add", "+", 3)
//    infixl("sub", "-", 3)
//    infixl("mul", "*", 4)
//    infixl("div", "/", 4)
//    infixr("pow", "^", 5)
//
//    collapse { name, _, operands -> when (name) {
//        "unm" -> -operands[0]
//        "lop" -> operands[0] - 1
//        "rop" -> operands[0] + 1
//        "add" -> operands[0] + operands[1]
//        "sub" -> operands[0] - operands[1]
//        "mul" -> operands[0] * operands[1]
//        "div" -> operands[0] / operands[1]
//        "pow" -> Math.pow(operands[0].toDouble(), operands[1].toDouble()).toInt()
//        else -> error("unsupported operator '$name'")
//    } }
//}
