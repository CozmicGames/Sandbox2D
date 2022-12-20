package engine.graphics.ui.widgets

import com.cozmicgames.utils.maths.Corners
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.drawRectFilled

/**
 * Adds a separator element.
 * The separator is a horizontal line the width of the given [element].
 *
 * @param element The element.
 */
fun GUI.separator(element: GUIElement) = separator(element.width)

/**
 * Adds a separator element.
 * The separator is a horizontal line of the given [width].
 *
 * @param width The width of the separator. Defaults to [style.elementSize] * 10.
 * @param element The element. Defaults to the last element.
 */
fun GUI.separator(width: Float = skin.elementSize * 10.0f): GUIElement {
    val (x, y) = getLastElement()
    val separatorX = x + skin.elementPadding
    val separatorY = y + skin.elementSize * 0.4f
    val separatorWidth = width - skin.elementPadding * 2.0f
    val separatorHeight = skin.elementSize * 0.2f
    currentCommandList.drawRectFilled(separatorX, separatorY, separatorWidth, separatorHeight, Corners.NONE, 0.0f, skin.normalColor)
    return setLastElement(x, y, width, skin.elementSize)
}
