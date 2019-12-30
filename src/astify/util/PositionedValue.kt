package astify.util

import astify.Position

data class PositionedValue<out T>(
        val value: T,
        val position: Position
) {
    infix fun <R> fmap(fn: (T) -> R): PositionedValue<R>
            = PositionedValue(fn(value), position)
}

infix fun <T> T.positioned(position: Position)
        = PositionedValue(this, position)
