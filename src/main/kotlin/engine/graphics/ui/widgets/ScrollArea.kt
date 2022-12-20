package engine.graphics.ui.widgets

import com.cozmicgames.Kore
import com.cozmicgames.input
import com.cozmicgames.utils.maths.Vector2
import engine.graphics.ui.*
import kotlin.math.min

enum class Scrollbar {
    NONE,
    HORIZONTAL,
    VERTICAL,
    BOTH;

    val hasHorizontalScrollbar get() = this == HORIZONTAL || this == BOTH
    val hasVerticalScrollbar get() = this == VERTICAL || this == BOTH
}

/**
 * Adds a scrollable area to the GUI.
 * A scroll area is a container of GUI elements that can be scrolled.
 *
 * @param maxWidth The maximum width of the scroll pane. If null, the width of the scroll pane is calculated automatically to fit all elements.
 * @param maxHeight The maximum height of the scroll pane. If null, the height of the scroll pane is calculated automatically to fit all elements.
 * @param scroll The current scroll position of the scroll pane. This function will update the scroll position automatically.
 * @param block The block to execute inside this area. It receives a [Scrollbar] value to check if and what scrollbars are shown.
 */
fun GUI.scrollArea(maxWidth: Float? = null, maxHeight: Float? = null, scroll: Vector2, block: (Scrollbar) -> Unit): GUIElement {
    val (x, y) = getLastElement()

    if (maxWidth == null)
        scroll.x = 0.0f

    if (maxHeight == null)
        scroll.y = 0.0f

    val contentSize = getElementSize { block(Scrollbar.NONE) }

    var contentWidth: Float
    var contentHeight: Float

    if (maxWidth == null) {
        contentWidth = contentSize.width
    } else {
        contentWidth = min(contentSize.width, maxWidth)

        if (scroll.x < 0.0f)
            scroll.x = 0.0f

        if (scroll.x > contentSize.width - contentWidth)
            scroll.x = contentSize.width - contentWidth
    }

    if (maxHeight == null) {
        contentHeight = contentSize.height
    } else {
        contentHeight = min(contentSize.height, maxHeight)

        if (scroll.y < 0.0f)
            scroll.y = 0.0f

        if (scroll.y > contentSize.height - contentHeight)
            scroll.y = contentSize.height - contentHeight
    }

    var totalWidth = contentWidth
    var totalHeight = contentHeight

    val showHorizontalScrollbar = contentWidth < contentSize.width
    val showVerticalScrollbar = contentHeight < contentSize.height

    if (showHorizontalScrollbar)
        contentWidth -= skin.scrollbarSize

    if (showVerticalScrollbar)
        contentHeight -= skin.scrollbarSize

    val scissorRectangle = getPooledRectangle()
    scissorRectangle.x = x
    scissorRectangle.y = y
    scissorRectangle.width = contentWidth
    scissorRectangle.height = contentHeight

    val previousScissorRectangle = currentScissorRectangle
    val previousUseScissorRectangleForElementPositioning = useScissorRectangleForElementPositioning

    currentScissorRectangle = scissorRectangle
    useScissorRectangleForElementPositioning = false

    val commands = recordCommands {
        transient(addToLayer = false) {
            setLastElement(absolute(x - scroll.x, y - scroll.y))
            block(
                when {
                    showHorizontalScrollbar && !showVerticalScrollbar -> Scrollbar.HORIZONTAL
                    !showHorizontalScrollbar && showVerticalScrollbar -> Scrollbar.VERTICAL
                    showHorizontalScrollbar && showVerticalScrollbar -> Scrollbar.BOTH
                    else -> Scrollbar.NONE
                }
            )
        }
    }

    currentScissorRectangle = previousScissorRectangle
    useScissorRectangleForElementPositioning = previousUseScissorRectangleForElementPositioning

    val rectangle = getPooledRectangle()
    rectangle.x = x
    rectangle.y = y
    rectangle.width = totalWidth
    rectangle.height = totalHeight

    if (GUI.State.HOVERED in getState(rectangle)) {
        if (maxWidth != null) scroll.x += currentScrollAmount.x
        if (maxHeight != null) scroll.y += currentScrollAmount.y
    }

    val scrollbarCommands = getPooledCommandList()

    if (showHorizontalScrollbar) {
        val scrollbarX = x
        val scrollbarY = y + contentHeight
        val scrollbarWidth = contentWidth
        val scrollbarHeight = skin.scrollbarSize

        val scrollbarGripX = scrollbarX + scroll.x * contentWidth / contentSize.width
        val scrollbarGripWidth = (contentWidth / contentSize.width) * scrollbarWidth

        val scrollbarState = getState(rectangle.apply {
            this.x = scrollbarX
            this.y = scrollbarY
            this.width = scrollbarWidth
            this.height = scrollbarHeight
        }, GUI.TouchBehaviour.NONE)

        val scrollbarGripState = getState(rectangle.apply {
            this.x = scrollbarGripX
            this.y = scrollbarY
            this.width = scrollbarGripWidth
            this.height = scrollbarHeight
        }, GUI.TouchBehaviour.REPEATED)

        val scrollbarColor = if (GUI.State.HOVERED in scrollbarState) skin.hoverColor else skin.normalColor

        val scrollbarGripColor = if (GUI.State.ACTIVE in scrollbarGripState && GUI.State.HOVERED in scrollbarGripState) {
            scroll.x += Kore.input.deltaX * contentSize.width / contentWidth
            skin.highlightColor
        } else if (GUI.State.HOVERED in scrollbarGripState)
            skin.normalColor
        else
            skin.backgroundColor

        scrollbarCommands.drawRectFilled(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, skin.roundedCorners, skin.cornerRounding, scrollbarColor)
        scrollbarCommands.drawRectFilled(scrollbarGripX, scrollbarY, scrollbarGripWidth, scrollbarHeight, skin.roundedCorners, skin.cornerRounding, scrollbarGripColor)

        totalHeight += scrollbarHeight
    } else
        scroll.x = 0.0f

    if (showVerticalScrollbar) {
        val scrollbarX = x + contentWidth
        val scrollbarY = y
        val scrollbarWidth = skin.scrollbarSize
        val scrollbarHeight = contentHeight

        val scrollbarGripY = scrollbarY + scroll.y * contentHeight / contentSize.height
        val scrollbarGripHeight = (contentHeight / contentSize.height) * scrollbarHeight

        val scrollbarState = getState(rectangle.apply {
            this.x = scrollbarX
            this.y = scrollbarY
            this.width = scrollbarWidth
            this.height = scrollbarHeight
        }, GUI.TouchBehaviour.NONE)

        val scrollbarGripState = getState(rectangle.apply {
            this.x = scrollbarX
            this.y = scrollbarGripY
            this.width = scrollbarWidth
            this.height = scrollbarGripHeight
        }, GUI.TouchBehaviour.REPEATED)

        val scrollbarColor = if (GUI.State.HOVERED in scrollbarState) skin.hoverColor else skin.normalColor

        val scrollbarGripColor = if (GUI.State.ACTIVE in scrollbarGripState && GUI.State.HOVERED in scrollbarGripState) {
            scroll.y -= Kore.input.deltaY * contentSize.height / contentHeight
            skin.highlightColor
        } else if (GUI.State.HOVERED in scrollbarGripState)
            skin.normalColor
        else
            skin.backgroundColor

        scrollbarCommands.drawRectFilled(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, skin.roundedCorners, skin.cornerRounding, scrollbarColor)
        scrollbarCommands.drawRectFilled(scrollbarX, scrollbarGripY, scrollbarWidth, scrollbarGripHeight, skin.roundedCorners, skin.cornerRounding, scrollbarGripColor)

        totalWidth += scrollbarWidth
    } else
        scroll.y = 0.0f

    currentCommandList.pushScissor(x, y, contentWidth, contentHeight)
    currentCommandList.addCommandList(commands)
    currentCommandList.popScissor()

    if (!scrollbarCommands.isEmpty)
        currentCommandList.addCommandList(scrollbarCommands)

    return setLastElement(x, y, totalWidth, totalHeight)
}
