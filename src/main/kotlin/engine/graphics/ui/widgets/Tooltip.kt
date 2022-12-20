package engine.graphics.ui.widgets

import com.cozmicgames.Kore
import com.cozmicgames.graphics
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.maths.Rectangle
import engine.graphics.ui.*

fun GUI.tooltip(element: GUIElement, text: String, backgroundColor: Color? = skin.backgroundColor): GUIElement {
    if (!shouldShowTooltip)
        return element

    topLayer {
        layerUp {
            val layout = getPooledGlyphLayout()
            layout.update(text, drawableFont)

            var x = touchPosition.x
            var y = touchPosition.y - (layout.height + skin.elementPadding * 2.0f)

            if (x + layout.width > Kore.graphics.width)
                x -= layout.width

            if (y + layout.height < 0.0f)
                y += layout.height

            val textX = x + skin.elementPadding
            val textY = y + skin.elementPadding
            val width = layout.width + 2.0f * skin.elementPadding
            val height = layout.height + 2.0f * skin.elementPadding

            val rectangle = Rectangle()
            rectangle.x = element.x
            rectangle.y = element.y
            rectangle.width = element.width
            rectangle.height = element.height

            if (GUI.State.HOVERED in getState(rectangle)) {
                topLayer {
                    if (backgroundColor != null)
                        currentCommandList.drawRectFilled(x, y, width, height, skin.roundedCorners, skin.cornerRounding, backgroundColor)

                    currentCommandList.drawText(textX, textY, layout, skin.fontColor)
                }
            }
        }
    }

    return element
}
