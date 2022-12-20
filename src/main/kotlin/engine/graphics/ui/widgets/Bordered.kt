package engine.graphics.ui.widgets

import com.cozmicgames.utils.Color
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.drawRect

/**
 *  Adds a border to the element returned by [block].
 *
 *  @param color The color of the border.
 *  @param thickness The thickness of the border.
 *  @param block The block to execute.
 */
fun GUI.bordered(color: Color, thickness: Float, block: () -> GUIElement): GUIElement {
    val element = block()
    currentCommandList.drawRect(element.x, element.y, element.width, element.height, skin.roundedCorners, skin.cornerRounding, thickness, color)
    return element
}
