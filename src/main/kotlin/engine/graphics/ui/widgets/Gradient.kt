package engine.graphics.ui.widgets

import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.drawRectMultiColor
import engine.utils.Gradient
import engine.utils.GradientColor

/**
 * Adds a gradient to the widget.
 *
 * @param gradient The gradient to add.
 * @param width The width of the gradient. Defaults to [style.elementSize] * 10.
 * @param height The height of the gradient. Defaults to [style.elementSize].
 *
 * TODO: Add gradient editor
 */
fun GUI.gradient(gradient: Gradient, width: Float = skin.elementSize * 10.0f, height: Float = skin.elementSize): GUIElement {
    val (x, y) = getLastElement()

    lateinit var last: GradientColor
    var isFirst = true

    for (color in gradient) {
        if (isFirst)
            isFirst = false
        else {
            val sliceX = color.stop * width
            val sliceWidth = (color.stop - last.stop) * width
            currentCommandList.drawRectMultiColor(sliceX, y, sliceWidth, height, last.color, last.color, color.color, color.color)
        }

        last = color
    }

    return setLastElement(x, y, width, height)
}
