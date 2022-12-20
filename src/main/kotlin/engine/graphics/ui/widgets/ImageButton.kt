package engine.graphics.ui.widgets

import com.cozmicgames.utils.Color
import com.cozmicgames.utils.maths.Corners
import engine.graphics.TextureRegion
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.drawImage
import engine.graphics.ui.drawRectFilled

/**
 * Adds an image button to the GUI.
 *
 * @param texture The texture to use for the image.
 * @param width The width of the image. Defaults to [style.elementSize].
 * @param height The height of the image. Defaults to the same as [width].
 * @param color The color of the image when not interacted with.
 * @param isEnabled If this button is enabled to be interacted with.
 * @param backgroundColor An optional color for the background of the image.
 * @param action The action to perform when the image is clicked.
 */
fun GUI.imageButton(texture: TextureRegion, width: Float = skin.elementSize, height: Float = width, color: Color = Color.WHITE, isEnabled: Boolean = true, backgroundColor: Color? = null, action: () -> Unit): GUIElement {
    val (x, y) = getLastElement()

    val rectangle = getPooledRectangle()
    rectangle.x = x
    rectangle.y = y
    rectangle.width = width
    rectangle.height = height

    val state = getState(rectangle, GUI.TouchBehaviour.ONCE_UP)

    val imageColor = if (isEnabled && GUI.State.ACTIVE in state) {
        action()
        skin.highlightColor
    } else if (isEnabled && GUI.State.HOVERED in state)
        skin.hoverColor
    else
        color

    if (backgroundColor != null)
        currentCommandList.drawRectFilled(x, y, width, height, Corners.NONE, 0.0f, backgroundColor)

    currentCommandList.drawImage(x, y, width, height, texture, imageColor)

    return setLastElement(x, y, width, height)
}
