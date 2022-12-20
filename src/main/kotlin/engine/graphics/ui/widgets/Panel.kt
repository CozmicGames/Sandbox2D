package engine.graphics.ui.widgets

import com.cozmicgames.utils.Color
import com.cozmicgames.utils.maths.Corners
import com.cozmicgames.utils.maths.Vector2
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.drawRectFilled

/**
 * Adds a panel to the GUI.
 * A panel represents a scrollable area of set size.
 *
 * @param width The width of the panel.
 * @param height The height of the panel.
 * @param scroll The current scroll position of the scroll pane. This function will update the scroll position automatically.
 * @param backgroundColor The panels' background color.
 * @param titleColor The titles' background color.
 * @param title A block of code to execute to display a title to this panel.
 * @param block A block of code that is executed as the body of this panel.
 */
fun GUI.panel(width: Float, height: Float, scroll: Vector2, backgroundColor: Color = skin.backgroundColor, titleColor: Color = skin.backgroundColor, title: (() -> GUIElement)? = null, block: (Scrollbar) -> Unit): GUIElement {
    val (x, y) = getLastElement()

    var titleHeight = 0.0f

    val element = group {
        if (title != null) {
            val titleCommands = recordCommands {
                titleHeight = title().height
            }

            currentCommandList.drawRectFilled(x, y, width, titleHeight, Corners.NONE, 0.0f, titleColor)
            currentCommandList.addCommandList(titleCommands)
        }

        lateinit var contentElement: GUIElement

        val commands = recordCommands {
            contentElement = scrollArea(width, height - titleHeight, scroll, block)
        }

        currentCommandList.drawRectFilled(contentElement.x, contentElement.y, width, height, Corners.NONE, 0.0f, backgroundColor)
        currentCommandList.addCommandList(commands)
    }

    return setLastElement(element.x, element.y, width, height)
}
