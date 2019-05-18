
/** Represents an object with a position */
abstract class Positioned<T> {
    /** Get the value of the object */
    abstract fun getValue(): T

    /** Get the position of the object */
    abstract fun getPosition(): Position

    /** Create a new positioned object with the same position but new value */
    fun <R> withValue(value: R): Positioned<R> {
        val thisCopy = this
        return object: Positioned<R>() {
            override fun getValue(): R = value
            override fun getPosition(): Position = thisCopy.getPosition()
        }
    }

    /** Create a new positioned object with the same position but a value mapped using `func` */
    fun <R> mapValue(func: (T) -> R): Positioned<R>
            = withValue(func(getValue()))

    /** Create a new positioned object with the same value but different position */
    fun withPosition(position: Position): Positioned<T> {
        val thisCopy = this
        return object: Positioned<T>() {
            override fun getValue(): T = thisCopy.getValue()
            override fun getPosition(): Position = position
        }
    }

    override fun toString(): String {
        return getValue().toString() + " @ " + getPosition().getPositionString()
    }
}
