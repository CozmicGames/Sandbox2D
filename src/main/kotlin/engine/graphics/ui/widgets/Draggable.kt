package engine.graphics.ui.widgets

import engine.graphics.ui.DragDropData
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.drawRect

/**
 * Marks the element returned by [block] as draggable.
 * If the element is clicked and dragged, a [DragDropData] instance is created by [createData].
 * If [borderThickness] is bigger than 0.0f, a border is displayed when the element returned by [block] is hovered or clicked.
 *
 * @param createData Creates the dragged data, containing the payload.
 * @param borderThickness Sets the thickness of the border when hovered or clicked.
 * @param block Its returned element will be marked as draggable.
 *
 * @return The element returned by [block].
 */
fun GUI.draggable(createData: () -> DragDropData<*>, borderThickness: Float = 0.0f, block: () -> GUIElement): GUIElement {
    val element = block()
    val rectangle = getPooledRectangle()
    rectangle.x = element.x
    rectangle.y = element.y
    rectangle.width = element.width
    rectangle.height = element.height

    if (currentDragDropData != null)
        return element

    val grabState = getState(rectangle, GUI.TouchBehaviour.ONCE_DOWN)

    val color = if (GUI.State.ACTIVE in grabState) {
        currentDragDropData = createData()
        skin.highlightColor
    } else if (GUI.State.HOVERED in grabState)
        skin.hoverColor
    else
        null

    if (borderThickness > 0.0f && color != null)
        currentCommandList.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height, skin.roundedCorners, skin.cornerRounding, borderThickness, color)

    return element
}
