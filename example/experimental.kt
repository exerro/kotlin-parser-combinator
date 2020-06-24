import astify.Position

sealed class L(open val position: Position) {
    data class A(override val position: Position): L(position)
}
