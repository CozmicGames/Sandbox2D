package engine.graphics.ui.widgets

import com.cozmicgames.Kore
import com.cozmicgames.graphics
import com.cozmicgames.input
import com.cozmicgames.utils.maths.Corners
import engine.graphics.ui.*
import kotlin.math.max
import kotlin.math.sin

/**
 * Adds a text field to the UI.
 *
 * @param textData The text data to use for the text field.
 * @param minWidth The minimum width of the text field. Should be more than 0, so it can be selected if the text is empty. Defaults to [style.elementSize].
 * @param action The action to perform when the text changed. Defaults to a no-op.
 */
fun GUI.textField(textData: TextData, minWidth: Float = skin.elementSize, action: () -> Unit = {}): GUIElement {
    val (x, y) = getLastElement()

    val layout = getPooledGlyphLayout()
    val rectangle = getPooledRectangle()

    layout.update(textData.text, drawableFont)

    rectangle.x = x
    rectangle.y = y
    rectangle.width = max(layout.width, minWidth) + skin.elementPadding * 2.0f
    rectangle.height = layout.height + skin.elementPadding * 2.0f

    val state = getState(rectangle, GUI.TouchBehaviour.REPEATED)

    if (GUI.State.HOVERED in state && GUI.State.ACTIVE in state) {
        currentTextData = textData
        val cursorIndex = layout.findCursorIndex(touchPosition.x - x + skin.elementPadding, touchPosition.y - y + skin.elementPadding)
        textData.setCursor(max(0, cursorIndex))
    } else if (Kore.input.justTouchedDown)
        currentTextData = null

    currentCommandList.drawRectFilled(rectangle.x, rectangle.y, rectangle.width, rectangle.height, skin.roundedCorners, skin.cornerRounding, skin.backgroundColor)
    currentCommandList.drawText(x + skin.elementPadding, y + skin.elementPadding, layout, textData.overrideFontColor ?: skin.fontColor)

    if (textData == currentTextData) {
        if (textData.isSelectionActive) {
            val selectionX: Float
            val selectionY: Float
            val selectionWidth: Float

            val cursor0 = textData.getFrontSelectionPosition()
            val cursor1 = textData.getEndSelectionPosition()

            if (cursor0 < layout.count) {
                val quad = layout[cursor0]
                selectionX = quad.x
                selectionY = quad.y
            } else {
                val quad = layout[layout.count - 1]
                selectionX = quad.x + quad.width
                selectionY = quad.y
            }

            selectionWidth = if (cursor1 < layout.count) {
                val quad = layout[cursor1]
                quad.x - selectionX
            } else {
                val quad = layout[layout.count - 1]
                quad.x + quad.width - selectionX
            }

            currentCommandList.drawRectFilled(x + selectionX + skin.elementPadding, y + selectionY + skin.elementPadding, selectionWidth, skin.elementSize, Corners.NONE, 0.0f, skin.cursorColor)
        } else if (sin(Kore.graphics.statistics.runTime * 5.0f) > 0.0f) {
            val cursorX: Float
            val cursorY: Float

            if (layout.count > 0) {
                if (textData.cursor < layout.count) {
                    val quad = layout[textData.cursor]
                    cursorX = quad.x
                    cursorY = quad.y
                } else {
                    val quad = layout[layout.count - 1]
                    cursorX = quad.x + quad.width
                    cursorY = quad.y
                }
            } else {
                cursorX = 0.0f
                cursorY = 0.0f
            }

            currentCommandList.drawRectFilled(x + cursorX + skin.elementPadding, y + cursorY + skin.elementPadding, 1.0f, skin.contentSize, Corners.NONE, 0.0f, skin.cursorColor)
        }
    }

    if (textData.hasChanged) {
        action()
        textData.hasChanged = false
    }

    return setLastElement(x, y, rectangle.width, rectangle.height)
}
