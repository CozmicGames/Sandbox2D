package engine.graphics.ui.widgets

import com.cozmicgames.utils.maths.Corners
import engine.graphics.ui.*
import kotlin.math.max

fun GUI.combobox(data: ComboboxData<*>, maxDropOutHeight: Float? = null): GUIElement {
    val layout = getPooledGlyphLayout()
    var maxItemWidth = 0.0f
    var itemsHeight = 0.0f

    data.forEach {
        layout.update(it.toString(), drawableFont)
        maxItemWidth = max(maxItemWidth, layout.width)
        itemsHeight += layout.height + skin.elementPadding * 2.0f
    }

    val requiresScrollbar = maxDropOutHeight != null && itemsHeight > maxDropOutHeight

    if (requiresScrollbar)
        maxItemWidth += skin.scrollbarSize + skin.elementPadding

    val element = dropdown(data.selectedItem.toString(), data.isOpen, maxItemWidth) {
        data.isOpen = it

        if (it) {
            if (currentComboBoxData != data)
                currentComboBoxData?.isOpen = false

            currentComboBoxData = data
        } else
            currentComboBoxData = null
    }

    maxItemWidth = max(maxItemWidth, element.width)

    if (requiresScrollbar)
        maxItemWidth -= skin.scrollbarSize + skin.elementPadding

    if (data.isOpen) {
        layerUp {
            transient(true) {
                scrollPane(maxHeight = maxDropOutHeight, scroll = data.scrollAmount, backgroundColor = skin.normalColor) {
                    repeat(data.size) {
                        comboboxElement(data, it, maxItemWidth)
                    }
                }
            }
        }
    }

    return element
}

private fun GUI.comboboxElement(data: ComboboxData<*>, index: Int, itemWidth: Float) {
    val (x, y) = getLastElement()

    val itemLayout = getPooledGlyphLayout()
    itemLayout.update(data[index].toString(), drawableFont)

    val rectangle = getPooledRectangle()

    rectangle.x = x
    rectangle.y = y
    rectangle.width = itemWidth + skin.elementPadding * 2.0f
    rectangle.height = itemLayout.height + skin.elementPadding + 2.0f

    val textX = x + skin.elementPadding
    val textY = y + skin.elementPadding

    val state = getState(rectangle, GUI.TouchBehaviour.ONCE_UP)

    val color = if (GUI.State.ACTIVE in state) {
        data.selectedIndex = index
        data.isOpen = false
        skin.highlightColor
    } else if (GUI.State.HOVERED in state)
        skin.hoverColor
    else
        null

    if (color != null)
        currentCommandList.drawRectFilled(rectangle.x, rectangle.y, rectangle.width, rectangle.height, Corners.NONE, 0.0f, color)

    currentCommandList.drawText(textX, textY, itemLayout, skin.fontColor)

    setLastElement(x, y, rectangle.width, rectangle.height)
}
