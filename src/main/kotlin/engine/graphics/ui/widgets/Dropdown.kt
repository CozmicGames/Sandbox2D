package engine.graphics.ui.widgets

import engine.graphics.ui.*
import kotlin.math.max

fun GUI.dropdown(title: String, isOpen: Boolean, minWidth: Float? = null, action: (Boolean) -> Unit): GUIElement {
    val (x, y) = getLastElement()

    val layout = getPooledGlyphLayout()
    layout.update(title, drawableFont)

    val textX = x + skin.elementPadding
    val textY = y + skin.elementPadding

    val requiredWidth = layout.width + skin.contentSize + skin.elementPadding * 3.0f
    val width = if (minWidth == null) requiredWidth else max(requiredWidth, minWidth)
    val height = layout.height + skin.elementPadding * 2.0f

    val dropdownX = x + width - skin.elementPadding - skin.contentSize
    val dropDownY = y + skin.elementPadding

    val rectangle = getPooledRectangle()
    rectangle.x = x
    rectangle.y = y
    rectangle.width = width
    rectangle.height = height

    val state = getState(rectangle, GUI.TouchBehaviour.ONCE_UP)

    val color = if (GUI.State.ACTIVE in state)
        skin.highlightColor
    else if (GUI.State.HOVERED in state)
        skin.hoverColor
    else
        skin.normalColor

    currentCommandList.drawRectFilled(x, y, width, height, skin.roundedCorners, skin.cornerRounding, color)
    currentCommandList.drawText(textX, textY, layout, skin.fontColor)

    val isClicked = GUI.State.ACTIVE in state
    var newOpen = isOpen

    if (isClicked) {
        newOpen = !newOpen
        action(newOpen)
    }

    if (newOpen) {
        val triangleX0 = dropdownX
        val triangleY0 = dropDownY + skin.contentSize

        val triangleX1 = dropdownX + skin.contentSize
        val triangleY1 = dropDownY + skin.contentSize

        val triangleX2 = dropdownX + skin.contentSize * 0.5f
        val triangleY2 = dropDownY

        currentCommandList.drawTriangleFilled(triangleX0, triangleY0, triangleX1, triangleY1, triangleX2, triangleY2, skin.fontColor)
    } else {
        val triangleX0 = dropdownX
        val triangleY0 = dropDownY

        val triangleX1 = dropdownX + skin.contentSize
        val triangleY1 = dropDownY

        val triangleX2 = dropdownX + skin.contentSize * 0.5f
        val triangleY2 = dropDownY + skin.contentSize

        currentCommandList.drawTriangleFilled(triangleX0, triangleY0, triangleX1, triangleY1, triangleX2, triangleY2, skin.fontColor)
    }

    return setLastElement(x, y, width, height)
}
