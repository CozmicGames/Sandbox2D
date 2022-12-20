package engine.graphics.ui.widgets

import com.cozmicgames.utils.Color
import engine.graphics.TextureRegion
import engine.graphics.font.HAlign
import engine.graphics.ui.*
import kotlin.math.max
import kotlin.math.min

/**
 * Adds a label element with an optional image.
 *
 * @param text The text of the label.
 * @param texture An optional texture to display behind the text.
 * @param backgroundColor The background color of the label. If null, no background is drawn.
 * @param maxWidth An optional maximum width of this label.
 * @param minWidth An optional minumum width of this label.
 * @param align How to align the text inside this label if [minWidth] exceeds the size needed by the text.
 * @param overrideFontColor A color that will be used for the text if not null.
 */
fun GUI.imageLabel(text: String, texture: TextureRegion? = null, backgroundColor: Color? = null, maxWidth: Float? = null, minWidth: Float? = null, align: HAlign = HAlign.LEFT, overrideFontColor: Color? = null): GUIElement {
    val (x, y) = getLastElement()
    val layout = getPooledGlyphLayout()
    layout.update(text, drawableFont)

    val textX = x + skin.elementPadding
    val textY = y + skin.elementPadding
    var textWidth = layout.width + 2.0f * skin.elementPadding
    val textHeight = layout.height + 2.0f * skin.elementPadding

    val fontColor = overrideFontColor ?: skin.fontColor

    if (maxWidth != null) {
        val clipRectangle = getPooledRectangle()
        clipRectangle.x = x
        clipRectangle.y = y
        clipRectangle.width = maxWidth
        clipRectangle.height = textHeight

        if (backgroundColor != null)
            currentCommandList.drawRectFilled(x, y, textWidth, textHeight, skin.roundedCorners, skin.cornerRounding, backgroundColor)

        currentCommandList.drawText(textX, textY, layout, fontColor, clipRectangle)

        textWidth = min(textWidth, maxWidth)
    } else if (minWidth != null) {
        val labelWidth = max(textWidth, minWidth)

        if (backgroundColor != null)
            currentCommandList.drawRectFilled(x, y, labelWidth, textHeight, skin.roundedCorners, skin.cornerRounding, backgroundColor)

        val alignedTextX = when (align) {
            HAlign.LEFT -> textX
            HAlign.CENTER -> textX + (labelWidth - textWidth) * 0.5f
            HAlign.RIGHT -> textX + labelWidth - textWidth
        }

        currentCommandList.drawText(alignedTextX, textY, layout, fontColor)

        if (texture != null)
            currentCommandList.drawImage(alignedTextX + layout.width + skin.elementPadding, y, skin.elementSize, skin.elementSize, texture, fontColor)

        textWidth = labelWidth
    } else {
        if (backgroundColor != null)
            currentCommandList.drawRectFilled(x, y, textWidth, textHeight, skin.roundedCorners, skin.cornerRounding, backgroundColor)

        currentCommandList.drawText(textX, textY, layout, fontColor)

        if (texture != null)
            currentCommandList.drawImage(textX + layout.width + skin.elementPadding, y, skin.elementSize, skin.elementSize, texture, fontColor)
    }

    return setLastElement(x, y, textWidth, textHeight)
}
