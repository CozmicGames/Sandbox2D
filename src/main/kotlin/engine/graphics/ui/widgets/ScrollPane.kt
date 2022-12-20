package engine.graphics.ui.widgets

import com.cozmicgames.utils.Color
import com.cozmicgames.utils.maths.Corners
import com.cozmicgames.utils.maths.Vector2
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.drawRectFilled

/**
 * Adds a scroll pane to the GUI.
 * A scroll pane is a container of GUI elements that can be scrolled.
 *
 * @param maxWidth The maximum width of the scroll pane. If null, the width of the scroll pane is calculated automatically to fit all elements.
 * @param maxHeight The maximum height of the scroll pane. If null, the height of the scroll pane is calculated automatically to fit all elements.
 * @param scroll The current scroll position of the scroll pane. This function will update the scroll position automatically.
 * @param backgroundColor The panels' background color.
 */
fun GUI.scrollPane(maxWidth: Float? = null, maxHeight: Float? = null, scroll: Vector2, backgroundColor: Color = skin.backgroundColor, block: (Scrollbar) -> Unit): GUIElement {
    lateinit var element: GUIElement

    val commands = recordCommands {
        element = scrollArea(maxWidth, maxHeight, scroll, block)
    }

    currentCommandList.drawRectFilled(element.x, element.y, element.width, element.height, Corners.NONE, 0.0f, backgroundColor)
    currentCommandList.addCommandList(commands)

    return element
}

