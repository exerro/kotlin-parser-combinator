import kotlin.reflect.KClass

sealed class ParserGeneratorOption

class ParserGenerator {
    private val parsers = mutableMapOf<KClass<*>, P<*>>()

    init {
        register(Int::class) { integer map { it.text.toInt() } }
        register(Float::class) { number map { it.text.toFloat() } }
        register(String::class) { identifier map { it.text } }
        register(Token::class) { token }
    }

    fun <T: Any> register(c: KClass<T>, vararg options: ParserGeneratorOption) {
        register(c) { genParser(c, options.toList()) }
    }

    fun <T: Any> register(c: KClass<T>, fn: parser.() -> P<T>) { parsers[c] = fn(parser) }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> get(c: KClass<T>): P<T> = parsers[c] as P<T>

    private fun <T: Any> genParser(c: KClass<T>, options: List<ParserGeneratorOption>): P<T> = parser {
        TODO()
    }
}

data class Hello(val z: Int, val h: Hello2)
data class Hello2(val x: Int, val y: Float)

fun main() {
    val pg = ParserGenerator()

//    pg.register(Hello::class)
//    pg.register(Hello2::class)
    println(pg.get(Int::class) parse "5")
}
