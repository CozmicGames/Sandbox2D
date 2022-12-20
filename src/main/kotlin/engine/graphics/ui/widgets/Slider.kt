package engine.graphics.ui.widgets

import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.drawCircleFilled
import engine.graphics.ui.drawRectFilled

/**
 * Adds a slider to the GUI.
 *
 * @param amount The amount of the slider. Must be between 0 and 1.
 * @param width The width of the slider. Defaults to [style.elementSize] * 10.
 * @param action The action to perform when the slider is changed. It is passed the new amount, between 0 and 1.
 */
fun GUI.slider(amount: Float, width: Float = skin.elementSize * 10.0f, action: (Float) -> Unit): GUIElement {
    val (x, y) = getLastElement()

    val handleRadius = skin.elementSize * 0.5f

    val sliderHeight = skin.elementSize / 3.0f
    val sliderWidth = width - handleRadius * 2.0f
    val sliderX = x + handleRadius
    val sliderY = y + sliderHeight

    var handleX = x + handleRadius + sliderWidth * amount
    val handleY = y + handleRadius

    currentCommandList.drawRectFilled(sliderX, sliderY, sliderWidth, sliderHeight, skin.roundedCorners, skin.cornerRounding, skin.normalColor)

    val rectangle = getPooledRectangle()
    rectangle.x = x
    rectangle.y = y
    rectangle.width = width
    rectangle.height = skin.elementSize

    val state = getState(rectangle, GUI.TouchBehaviour.REPEATED)

    if (GUI.State.HOVERED in state) {
        val color = if (GUI.State.ACTIVE in state) {
            val newAmount = (touchPosition.x - x) / width
            handleX = x + handleRadius + sliderWidth * newAmount

            action(newAmount)
            skin.highlightColor
        } else
            skin.hoverColor

        currentCommandList.drawCircleFilled(handleX, handleY, handleRadius, color)
    } else
        currentCommandList.drawCircleFilled(handleX, handleY, handleRadius, skin.normalColor)

    return setLastElement(x, y, width, skin.elementSize)
}
