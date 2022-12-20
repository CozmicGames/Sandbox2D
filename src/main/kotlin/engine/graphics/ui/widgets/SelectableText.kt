package engine.graphics.ui.widgets

import engine.graphics.TextureRegion
import engine.graphics.ui.*

/**
 * Adds a selectable text to the GUI.
 *
 * @param text The text.
 * @param texture An optional texture to display behind the text.
 * @param isSelected Whether the image is selected.
 * @param action The action to perform when the image is clicked.
 */
fun GUI.selectableText(text: String, texture: TextureRegion? = null, isSelected: Boolean, action: () -> Unit): GUIElement {
    val (x, y) = getLastElement()

    val rectangle = getPooledRectangle()
    rectangle.x = x
    rectangle.y = y

    val layout = getPooledGlyphLayout()
    layout.update(text, drawableFont)

    val textX = x + skin.elementPadding
    val textY = y + skin.elementPadding

    rectangle.width = layout.width + skin.elementPadding * 2.0f
    rectangle.height = layout.height + skin.elementPadding * 2.0f

    if (texture != null)
        rectangle.width += skin.elementSize

    val state = getState(rectangle, GUI.TouchBehaviour.ONCE_DOWN)

    if (GUI.State.ACTIVE in state)
        action()

    val color = if (GUI.State.HOVERED in state && !isSelected)
        skin.hoverColor
    else if (isSelected)
        skin.highlightColor
    else
        skin.normalColor

    currentCommandList.drawRectFilled(rectangle.x, rectangle.y, rectangle.width, rectangle.height, skin.roundedCorners, skin.cornerRounding, color)
    currentCommandList.drawText(textX, textY, layout, skin.fontColor)

    if (texture != null)
        currentCommandList.drawImage(textX + layout.width + skin.elementPadding, y, skin.elementSize, skin.elementSize, texture, skin.fontColor)

    return setLastElement(x, y, rectangle.width, rectangle.height)
}
