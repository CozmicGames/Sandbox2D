package engine.graphics.ui.widgets

import com.cozmicgames.utils.Color
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.drawRectFilled

/**
 * Adds a color rectangle element.
 *
 * @param color The color.
 * @param width The width of the rectangle. Defaults to [style.elementSize].
 * @param height The height of the rectangle. Defaults to the same value as [width].
 */
fun GUI.colorRectangle(color: Color, width: Float = skin.elementSize, height: Float = width): GUIElement {
    val (x, y) = getLastElement()
    currentCommandList.drawRectFilled(x, y, width, height, skin.roundedCorners, skin.cornerRounding, color)
    return setLastElement(x, y, width, height)
}
