package astify.util

sealed class Either<out A, out B> {
    class Left<A>(val value: A): Either<A, Nothing>()
    class Right<B>(val value: B): Either<Nothing, B>()

    fun left(): A? = when (this) { is Left -> value; else -> null }
    fun right(): B? = when (this) { is Right -> value; else -> null }

    fun <T> mapLeft(fn: (A) -> T): Either<T, B>
            = when (this) { is Left -> Left(fn(value)); is Right -> this }

    fun <T> mapRight(fn: (B) -> T): Either<A, T>
            = when (this) { is Left -> this; is Right -> Right(fn(value)) }
}
