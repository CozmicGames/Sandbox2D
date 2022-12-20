package engine.graphics.ui.widgets

import com.cozmicgames.utils.Color
import engine.graphics.TextureRegion
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.drawImage
import engine.graphics.ui.drawRect

/**
 * Adds an image element.
 *
 * @param texture The texture of the image.
 * @param width The width of the image. Defaults to [style.elementSize].
 * @param height The height of the image. Defaults to the same value as [width].
 * @param color The color to tint the image with. Defaults to [Color.WHITE].
 * @param borderThickness The thickness of the border. Defaults to 4.0f.
 * @param borderInside Whether the border should be inside or outside the specified [width] and [height].
 */
fun GUI.image(texture: TextureRegion, width: Float = skin.elementSize, height: Float = width, color: Color = Color.WHITE, borderThickness: Float = skin.strokeThickness, borderInside: Boolean = true): GUIElement {
    val (x, y) = getLastElement()

    currentCommandList.drawImage(x, y, width, height, texture, color)

    if (borderThickness > 0.0f) {
        if (borderInside)
            currentCommandList.drawRect(x, y, width, height, skin.roundedCorners, skin.cornerRounding, borderThickness, skin.normalColor)
        else
            currentCommandList.drawRect(x - borderThickness, y - borderThickness, width + borderThickness * 2.0f, height + borderThickness * 2.0f, skin.roundedCorners, skin.cornerRounding, borderThickness, skin.normalColor)
    }

    return setLastElement(x, y, width, height)
}
