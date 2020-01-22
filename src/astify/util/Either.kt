package astify.util

sealed class Either<out A, out B> {
    data class Left<A>(val value: A): Either<A, Nothing>()
    data class Right<B>(val value: B): Either<Nothing, B>()

    fun left(): A? = when (this) { is Left -> value; else -> null }
    fun right(): B? = when (this) { is Right -> value; else -> null }

    fun <T> mapLeft(fn: (A) -> T): Either<T, B>
            = when (this) { is Left -> Left(fn(value)); is Right -> this }

    fun <T> mapRight(fn: (B) -> T): Either<A, T>
            = when (this) { is Left -> this; is Right -> Right(fn(value)) }
}

fun <A, B, T> Either<A, B>.flatMapLeft(fn: (A) -> Either<T, B>): Either<T, B>
    = when (this) { is Either.Left -> fn(value); is Either.Right -> this }

fun <A, B, T> Either<A, B>.flatMapRight(fn: (B) -> Either<A, T>): Either<A, T>
        = when (this) { is Either.Left -> this; is Either.Right -> fn(value) }

fun <T> Either<T, T>.value() = when (this) {
    is Either.Left -> value
    is Either.Right -> value
}
