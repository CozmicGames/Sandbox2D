package engine.graphics.ui

import com.cozmicgames.Kore
import com.cozmicgames.input
import com.cozmicgames.input.Key
import com.cozmicgames.input.Keys
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.extensions.clamp
import kotlin.math.max
import kotlin.math.min

open class TextData(text: String = "", var onEnter: TextData.() -> Unit = {}) {
    var overrideFontColor: Color? = null

    var text = text
        private set

    var cursor = -1
        internal set

    var selectionLength = 0
        internal set

    internal var hasChanged = false

    val isSelectionActive get() = text.length > 1 && selectionLength != 0

    fun onKeyAction(key: Key) {
        if (cursor >= 0) {
            when (key) {
                Keys.KEY_LEFT -> if (Kore.input.isKeyDown(Keys.KEY_CONTROL)) moveSelection(-1) else moveCursor(-1)
                Keys.KEY_RIGHT -> if (Kore.input.isKeyDown(Keys.KEY_CONTROL)) moveSelection(1) else moveCursor(1)
                Keys.KEY_BACKSPACE -> if (isSelectionActive) removeSelection() else removeCharBeforeCursor()
                Keys.KEY_DELETE -> if (isSelectionActive) removeSelection() else removeCharAfterCursor()
                Keys.KEY_ENTER -> onEnter(this)
            }
        }

        if (Kore.input.isKeyDown(Keys.KEY_CONTROL) && key == Keys.KEY_V) {
            val clipboard = Kore.clipboard
            if (clipboard != null) {
                if (isSelectionActive)
                    removeSelection(clipboard)
                else
                    addTextAtCursor(clipboard)
            }
        }

        if (Kore.input.isKeyDown(Keys.KEY_CONTROL) && key == Keys.KEY_C)
            Kore.clipboard = getSelection()

        if (Kore.input.isKeyDown(Keys.KEY_CONTROL) && key == Keys.KEY_X)
            Kore.clipboard = removeSelection()
    }

    fun onCharAction(char: Char) {
        addTextAtCursor(char.toString())
    }

    fun setText(text: String) {
        this.text = text
        setCursor(0)
    }

    fun setCursor(position: Int) {
        cursor = position.clamp(0, text.length)
        resetSelection()
    }

    fun resetSelection() {
        selectionLength = 0
    }

    fun moveCursor(amount: Int) {
        if (isSelectionActive)
            cursor = getFrontSelectionPosition() + amount
        else
            cursor += amount

        resetSelection()

        cursor = cursor.clamp(0, text.length)
    }

    fun moveSelection(amount: Int) {
        selectionLength += amount

        if (cursor + selectionLength < 0)
            selectionLength = -cursor
        else if (cursor + selectionLength > text.length)
            selectionLength = text.length - cursor
    }

    fun addTextAtCursor(text: String) {
        when (cursor) {
            0 -> this.text = text + this.text
            this.text.length -> this.text = this.text + text
            else -> this.text = this.text.substring(0, cursor) + text + this.text.substring(cursor, this.text.length)
        }

        cursor += text.length

        hasChanged = true
    }

    fun removeCharBeforeCursor() {
        if (cursor > 0) {
            text = if (cursor < text.length)
                text.substring(0, cursor - 1) + text.substring(cursor, text.length)
            else
                text.substring(0, text.length - 1)

            cursor--

            hasChanged = true
        }
    }

    fun removeCharAfterCursor() {
        if (cursor < text.length) {
            if (cursor > 0)
                text = text.substring(0, cursor) + text.substring(cursor + 1, text.length)
            else if (text.length > 1)
                text = text.substring(1, text.length)
            else if (text.length == 1)
                text = ""

            hasChanged = true
        }
    }

    fun removeSelection(replacement: String = ""): String {
        if (!isSelectionActive)
            return ""

        val selectedString = getSelection()
        if (replacement == selectedString)
            return selectedString

        val cursor0 = getFrontSelectionPosition()
        val cursor1 = getEndSelectionPosition()

        text = text.substring(0, cursor0) + replacement + text.substring(cursor1, text.length)

        hasChanged = true

        cursor = cursor0 + replacement.length
        resetSelection()

        return selectedString
    }

    fun getSelection(): String {
        if (!isSelectionActive)
            return ""

        val cursor0 = getFrontSelectionPosition()
        val cursor1 = getEndSelectionPosition()

        return text.substring(cursor0, cursor1)
    }

    fun getFrontSelectionPosition() = min(cursor, cursor + selectionLength)

    fun getEndSelectionPosition() = max(cursor, cursor + selectionLength)
}