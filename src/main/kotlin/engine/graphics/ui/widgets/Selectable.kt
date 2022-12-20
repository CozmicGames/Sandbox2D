package engine.graphics.ui.widgets

import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.drawRect

/**
 * Adds a selectable element to the GUI.
 *
 * @param block The block of code to execute to retrieve the element which should be selectable.
 * @param isSelected Whether the image is selected.
 * @param action The action to perform when the image is clicked.
 */
fun GUI.selectable(block: () -> GUIElement, isSelected: Boolean, action: () -> Unit): GUIElement {
    val element = block()

    val rectangle = getPooledRectangle()
    rectangle.x = element.x
    rectangle.y = element.y
    rectangle.width = element.width
    rectangle.height = element.height

    val state = getState(rectangle, GUI.TouchBehaviour.ONCE_DOWN)

    if (GUI.State.ACTIVE in state)
        action()

    if (GUI.State.HOVERED in state && !isSelected)
        currentCommandList.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height, skin.roundedCorners, skin.cornerRounding, skin.strokeThickness, skin.hoverColor)
    else if (isSelected)
        currentCommandList.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height, skin.roundedCorners, skin.cornerRounding, skin.strokeThickness, skin.highlightColor)

    return element
}
