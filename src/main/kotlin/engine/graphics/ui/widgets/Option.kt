package engine.graphics.ui.widgets

import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.drawCircleFilled

/**
 * Adds an option to the GUI.
 *
 * @param option The id of the option.
 * @param selectedOption The id of the selected option.
 * @param action The action to perform when the option is selected. It receives the option id as a parameter.
 */
fun GUI.option(option: Int, selectedOption: Int, action: (Int) -> Unit): GUIElement {
    val (x, y) = getLastElement()
    val size = skin.elementSize

    val rectangle = getPooledRectangle()
    rectangle.x = x
    rectangle.y = y
    rectangle.width = size
    rectangle.height = size

    val state = getState(rectangle, GUI.TouchBehaviour.ONCE_UP)

    currentCommandList.drawCircleFilled(x + size * 0.5f, y + size * 0.5f, size * 0.5f, if (GUI.State.HOVERED in state) skin.hoverColor else skin.normalColor)
    val isClicked = GUI.State.ACTIVE in state
    var newChecked = option == selectedOption

    if (isClicked) {
        newChecked = !newChecked
        action(option)
    }

    if (newChecked) {
        val middleX = x + skin.elementPadding
        val middleY = y + skin.elementPadding
        val middleSize = skin.contentSize

        currentCommandList.drawCircleFilled(middleX + middleSize * 0.5f, middleY + middleSize * 0.5f, middleSize * 0.5f, skin.highlightColor)
    }

    return setLastElement(x, y, size, size)
}
