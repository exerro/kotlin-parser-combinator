
sealed class Either<out A, out B> {
    class Left<A>(val value: A): Either<A, Nothing>()
    class Right<B>(val value: B): Either<Nothing, B>()
}

fun <A, B> Either<A, B>.isl(): Boolean = this is Either.Left<A>
fun <A, B> Either<A, B>.getl(): A = if (this is Either.Left<A>) this.value else throw IllegalAccessError("Attempt to get left value of Either.Right")

fun <A, B> Either<A, B>.isr(): Boolean = this is Either.Right<B>
fun <A, B> Either<A, B>.getr(): B = if (this is Either.Right<B>) this.value else throw IllegalAccessError("Attempt to get right value of Either.Left")
