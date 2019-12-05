package astify.util

import astify.Position

data class ValuePositionPair<out T>(
        val value: T,
        val position: Position
)

infix fun <T> T.positioned(position: Position)
        = ValuePositionPair(this, position)
