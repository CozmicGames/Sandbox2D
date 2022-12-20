package engine.graphics.ui

import com.cozmicgames.utils.maths.Vector2

class ComboboxData<T : Any>(vararg values: T) : Iterable<T> {
    var isOpen = false
        internal set(value) {
            if (!value)
                scrollAmount.setZero()

            field = value
        }

    private val items = arrayListOf<T>()

    var selectedIndex = 0
        internal set

    val selectedItem get() = items.getOrNull(selectedIndex)

    val scrollAmount = Vector2()

    val size get() = items.size

    init {
        values.forEach(::addItem)
    }

    fun addItem(item: T) {
        items.add(item)
    }

    fun removeItem(item: T) {
        items.remove(item)
    }

    fun clear() {
        items.clear()
    }

    operator fun get(index: Int) = items[index]

    override fun iterator(): Iterator<T> = items.iterator()
}