package engine.graphics.ui.widgets

import com.cozmicgames.utils.Color
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.drawRectFilled

/**
 *  Adds a drop shadow to the element returned by [block].
 *
 *  @param color The color of the shadow.
 *  @param offsetX The offset in the x direction.
 *  @param offsetY The offset in the y direction.
 *  @param block The block to execute.
 */
fun GUI.dropShadow(color: Color, offsetX: Float = skin.elementSize * 0.33f, offsetY: Float = offsetX, block: () -> GUIElement): GUIElement {
    lateinit var element: GUIElement
    val commands = recordCommands {
        element = block()
    }

    currentCommandList.drawRectFilled(element.x + offsetX, element.y + offsetY, element.width, element.height, skin.roundedCorners, skin.cornerRounding, color)
    currentCommandList.addCommandList(commands)

    return element
}
