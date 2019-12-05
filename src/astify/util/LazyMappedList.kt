package astify.util

internal class LazyMappedList<T, R>(
        private val source: List<T>,
        private val ofn: (T) -> R
) {
    private val items: MutableList<R> = mutableListOf()

    fun first(fn: (R) -> Boolean): R? {
        items.firstOrNull(fn) ?.let { return it }

        while (items.size < source.size) {
            val next = ofn(source[items.size])
            items.add(next)
            if (fn(next)) return next
        }

        return null
    }

    fun all(): List<R> {
        while (items.size < source.size) {
            items.add(ofn(source[items.size]))
        }

        return items
    }
}
