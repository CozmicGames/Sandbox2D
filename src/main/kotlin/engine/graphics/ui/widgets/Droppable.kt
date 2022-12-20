package engine.graphics.ui.widgets

import engine.graphics.ui.DragDropData
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.drawRect
import kotlin.reflect.KClass

/**
 * Marks the element returned by [block] as droppable.
 * If there is currently a [DragDropData] instance active, and it's payload is an instance of [acceptedType], it can be dropped to this.
 * Also, if there are externally dropped elements inside the bounds of the element returned by [block] and [acceptedType] allows it, they can be dropped to this.
 * For this, [acceptedType] needs to be a [List] of strings or [String], for which only the first element will be dropped.
 *
 * If there is something dropped to this, the function will call [onDrop] with the dropped element.
 *
 * If [borderThickness] is bigger than 0.0f, a border is displayed when the element returned by [block] is hovered and something droppable is accepted.
 *
 * @param onDrop Is called when something is dropped on this.
 * @param acceptedType The type required for dropped elements to be accepted by this.
 * @param borderThickness Sets the thickness of the border when hovered.
 * @param block Its returned element will be marked as droppable.
 *
 * @return The element returned by [block].
 */
fun <T : Any> GUI.droppable(onDrop: (T) -> Unit, acceptedType: KClass<T>, borderThickness: Float = 0.0f, block: () -> GUIElement): GUIElement {
    val element = block()
    val rectangle = getPooledRectangle()
    rectangle.x = element.x
    rectangle.y = element.y
    rectangle.width = element.width
    rectangle.height = element.height

    val state = getState(rectangle, GUI.TouchBehaviour.ONCE_UP, false)

    val isDragDropDataAccepted = currentDragDropData != null && acceptedType.isInstance(currentDragDropData?.payload)
    val isExternalDropElementsAccepted = externalDroppedElements.isNotEmpty() && acceptedType.isInstance(externalDroppedElements)
    val isExternalDropSingleElementAccepted = externalDroppedElements.isNotEmpty() && acceptedType.isInstance(externalDroppedElements.first())

    val isExternalAccepted = isExternalDropElementsAccepted || isExternalDropSingleElementAccepted
    val isAnyAccepted = isDragDropDataAccepted || isExternalAccepted

    val color = if (GUI.State.ACTIVE_DROPPABLE in state) {
        if (isDragDropDataAccepted)
            onDrop(requireNotNull(currentDragDropData).payload as T)

        skin.highlightColor
    } else if (GUI.State.HOVERED_DROPPABLE in state)
        skin.hoverColor
    else if (isExternalAccepted && isInteractionEnabled && externalDropLocation in rectangle && isPositionVisible(externalDropLocation) && isPositionInsideCurrentScissorRect(externalDropLocation)) {
        if (isExternalDropElementsAccepted) {
            onDrop(externalDroppedElements as T)
            externalDroppedElements.clear()
        }

        if (isExternalDropSingleElementAccepted) {
            onDrop(externalDroppedElements.first() as T)
            externalDroppedElements.clear()
        }

        skin.highlightColor
    } else if (isAnyAccepted)
        skin.normalColor
    else
        null

    if (isAnyAccepted && borderThickness > 0.0f && color != null)
        currentCommandList.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height, skin.roundedCorners, skin.cornerRounding, borderThickness, color)

    return element
}

inline fun <reified T : Any> GUI.droppable(noinline onDrop: (T) -> Unit, borderThickness: Float = 0.0f, noinline block: () -> GUIElement) = droppable(onDrop, T::class, borderThickness, block)
