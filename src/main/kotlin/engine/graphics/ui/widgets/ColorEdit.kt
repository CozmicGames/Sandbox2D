package engine.graphics.ui.widgets

import com.cozmicgames.utils.Color
import com.cozmicgames.utils.maths.Corners
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.drawRectFilled
import engine.graphics.ui.drawRectMultiColor

/**
 * Adds a color picker to the GUI.
 *
 * @param color The color to edit.
 * @param onChange Is called when the color is changed.
 */
fun GUI.colorEdit(color: Color, onChange: () -> Unit = {}): GUIElement {
    val (x, y) = getLastElement()

    val hsv = color.toHSV()
    var newAlpha = color.a
    val rectangle = getPooledRectangle()
    val size = 100.0f
    var totalWidth = 0.0f
    var totalHeight = 0.0f

    val commands = recordCommands {
        rectangle.x = x + skin.elementPadding
        rectangle.y = y + skin.elementPadding + 15.0f
        rectangle.width = size
        rectangle.height = size

        currentCommandList.drawRectMultiColor(rectangle.x + rectangle.width * 0.5f, rectangle.y + rectangle.height * 0.5f, rectangle.width, rectangle.height, Color.WHITE, color, color, Color.WHITE)
        currentCommandList.drawRectMultiColor(rectangle.x + rectangle.width * 0.5f, rectangle.y + rectangle.height * 0.5f, rectangle.width, rectangle.height, Color.CLEAR, Color.CLEAR, Color.BLACK, Color.BLACK)

        var state = getState(rectangle, GUI.TouchBehaviour.REPEATED)

        var crossHairColor = skin.normalColor
        if (GUI.State.HOVERED in state && currentDragDropData == null) {
            if (GUI.State.ACTIVE in state) {
                hsv[1] = (touchPosition.x - rectangle.x) / rectangle.width
                hsv[2] = 1.0f - (touchPosition.y - rectangle.y) / rectangle.height
                crossHairColor = skin.highlightColor
            } else
                crossHairColor = skin.hoverColor
        }

        val crossHairX = rectangle.x + hsv[1] * rectangle.width
        val crossHairY = rectangle.y + (1.0f - hsv[2]) * rectangle.height

        currentCommandList.drawRectFilled(crossHairX - 5.0f, crossHairY - 1.0f, 10.0f, 2.0f, Corners.NONE, 0.0f, crossHairColor)
        currentCommandList.drawRectFilled(crossHairX - 1.0f, crossHairY - 5.0f, 2.0f, 10.0f, Corners.NONE, 0.0f, crossHairColor)

        rectangle.x += 105.0f
        rectangle.width = 20.0f

        val hueBarX = rectangle.x + skin.elementPadding
        val hueBarY = rectangle.y
        val hueBarWidth = rectangle.width - skin.elementPadding * 2.0f
        val hueBarSubHeight = rectangle.height / Color.HUE_COLORS.size

        Color.HUE_COLORS.forEachIndexed { index, c0 ->
            val c1 = Color.HUE_COLORS[if (index < Color.HUE_COLORS.lastIndex) index + 1 else 0]
            currentCommandList.drawRectMultiColor(hueBarX + hueBarWidth * 0.5f, hueBarY + index * hueBarSubHeight + hueBarSubHeight * 0.5f, hueBarWidth, hueBarSubHeight, c0, c0, c1, c1)
        }

        state = getState(rectangle, GUI.TouchBehaviour.REPEATED)

        var hueLineColor = skin.normalColor
        if (GUI.State.HOVERED in state) {
            if (GUI.State.ACTIVE in state) {
                hsv[0] = (touchPosition.y - rectangle.y) / rectangle.height * 360.0f
                hueLineColor = skin.highlightColor
            } else
                hueLineColor = skin.hoverColor
        }

        val hueLineY = rectangle.y + (hsv[0] / 360.0f) * rectangle.height - 0.5f - 2.0f
        currentCommandList.drawRectFilled(rectangle.x, hueLineY, rectangle.width, 4.0f, Corners.NONE, 0.0f, hueLineColor)

        rectangle.x += 25.0f
        rectangle.width = 20.0f

        val alphaBarX = rectangle.x + skin.elementPadding
        val alphaBarY = rectangle.y
        val alphaBarWidth = rectangle.width - skin.elementPadding * 2.0f
        val alphaBarHeight = rectangle.height

        currentCommandList.drawRectMultiColor(alphaBarX + alphaBarWidth * 0.5f, alphaBarY + alphaBarHeight * 0.5f, alphaBarWidth, alphaBarHeight, Color.WHITE, Color.WHITE, Color.BLACK, Color.BLACK)

        state = getState(rectangle, GUI.TouchBehaviour.REPEATED)

        var alphaLineColor = skin.normalColor
        if (GUI.State.HOVERED in state) {
            if (GUI.State.ACTIVE in state) {
                newAlpha = 1.0f - (touchPosition.y - rectangle.y) / rectangle.height
                alphaLineColor = skin.highlightColor
            } else
                alphaLineColor = skin.hoverColor
        }

        val alphaLineY = rectangle.y + (1.0f - color.a) * rectangle.height - 0.5f - 2.0f
        currentCommandList.drawRectFilled(rectangle.x, alphaLineY, rectangle.width, 4.0f, Corners.NONE, 0.0f, alphaLineColor)

        totalWidth = rectangle.x + rectangle.width - (x + skin.elementPadding)
        totalHeight = rectangle.y + rectangle.height - (y + skin.elementPadding)
        currentCommandList.drawRectFilled(x + skin.elementPadding, y + skin.elementPadding, totalWidth, 12.0f, skin.roundedCorners, skin.cornerRounding, color)
    }

    val newColor = Color()
    newColor.fromHSV(hsv)
    newColor.a = newAlpha

    if (newColor != color) {
        onChange()
        color.set(newColor)
    }

    currentCommandList.drawRectFilled(x, y, totalWidth + skin.elementPadding * 2.0f, totalHeight + skin.elementPadding * 2.0f, skin.roundedCorners, skin.cornerRounding, skin.backgroundColor)
    currentCommandList.addCommandList(commands)

    return setLastElement(x, y, totalWidth + skin.elementPadding * 2.0f, totalHeight + skin.elementPadding * 2.0f)
}
