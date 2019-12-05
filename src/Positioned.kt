
/** Represents an object with a position */
@Deprecated("Use ASTify instead")
abstract class Positioned<out T> {
    /** Get the value of the object */
    abstract fun getValue(): T

    /** Get the position of the object */
    abstract fun getPosition(): Position

    /** Create a new positioned object with the same position but new value */
    fun <R> withValue(value: R): Positioned<R> = positioned(value, getPosition())

    /** Create a new positioned object with the same position but a value mapped using `func` */
    fun <R> mapValue(func: (T) -> R): Positioned<R> = withValue(func(getValue()))

    /** Create a new positioned object with the same value but different position */
    fun withPosition(position: Position): Positioned<T> = positioned(getValue(), position)

    override fun toString(): String = getValue().toString() + " @ " + getPosition().getPositionString()
}

/** Create a positioned object */
fun <T> positioned(value: T, position: Position) = object: Positioned<T>() {
    override fun getValue(): T = value
    override fun getPosition(): Position = position
}
