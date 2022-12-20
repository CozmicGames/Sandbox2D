package engine.graphics.ui.widgets

import com.cozmicgames.utils.Color
import engine.graphics.TextureRegion
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.drawImage
import engine.graphics.ui.drawRect

/**
 * Adds a selectable image to the GUI.
 *
 * @param texture The texture to use for the image.
 * @param width The width of the image. Defaults to [style.elementSize].
 * @param height The height of the image. Defaults to the same as [width].
 * @param isSelected Whether the image is selected.
 * @param action The action to perform when the image is clicked.
 */
fun GUI.selectableImage(texture: TextureRegion, width: Float = skin.elementSize, height: Float = width, isSelected: Boolean, action: () -> Unit): GUIElement {
    val (x, y) = getLastElement()

    val rectangle = getPooledRectangle()
    rectangle.x = x
    rectangle.y = y
    rectangle.width = width
    rectangle.height = height

    val state = getState(rectangle, GUI.TouchBehaviour.ONCE_DOWN)

    currentCommandList.drawImage(x, y, width, height, texture, Color.WHITE)

    if (GUI.State.ACTIVE in state)
        action()

    if (GUI.State.HOVERED in state && !isSelected)
        currentCommandList.drawRect(x, y, width, height, skin.roundedCorners, skin.cornerRounding, skin.strokeThickness, skin.hoverColor)
    else if (isSelected)
        currentCommandList.drawRect(x, y, width, height, skin.roundedCorners, skin.cornerRounding, skin.strokeThickness, skin.highlightColor)

    return setLastElement(x, y, width, height)
}
