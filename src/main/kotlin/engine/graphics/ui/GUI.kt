package engine.graphics.ui

import com.cozmicgames.*
import com.cozmicgames.graphics.safeHeight
import com.cozmicgames.graphics.safeWidth
import com.cozmicgames.input.*
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.Time
import com.cozmicgames.utils.collections.ReclaimingPool
import com.cozmicgames.utils.maths.*
import engine.Game
import engine.graphics.MatrixStack
import engine.graphics.font.GlyphLayout
import engine.graphics.font.SDFFont
import engine.graphics.shaders.DefaultShader
import engine.input.GestureListener
import kotlin.math.max
import kotlin.math.min

class GUI(val skin: GUISkin = GUISkin()) : Disposable {
    enum class State {
        HOVERED,
        ACTIVE,
        ENTERED,
        LEFT,
        HOVERED_DROPPABLE,
        ACTIVE_DROPPABLE,
        DOUBLE_TAP;

        companion object {
            /**
             * Combines the given states into a bitfield.
             * @param states The states to combine.
             * @param base The base state.
             *
             * @return The resulting bitfield.
             */
            fun combine(vararg states: State, base: Int = 0): Int {
                var flags = base
                states.forEach {
                    /**
                     * The state an element can have.
                     * HOVERED: The mouse is hovering over the element.
                     * ACTIVE: The element is being interacted with.
                     * ENTERED: The element has been entered.
                     * LEFT: The element has been left.
                     * HOVERED_DROPPABLE: The mouse is hovering over the element and either [currentDragDropData] is not null or external dropped elements are present.
                     * ACTIVE_WITH_DRAGDROPDATA: The element is being interacted with and either [currentDragDropData] is not null or external dropped elements are present.
                     * DOUBLE_TAP: The element is being interacted with and it is double-tapped.
                     *
                     * States can be combined to a bitfield using the combine function.
                     * The resulting bitfield can be used to check if a state is active by the isSet function.
                     */
                    flags = flags or (1 shl it.ordinal)
                }
                return flags
            }

            /**
             * Checks if the given state is set in the bitfield.
             * @param flags The bitfield to check.
             * @param state The state to check for.
             *
             * @return True if the state is set, false otherwise.
             */
            fun isSet(flags: Int, state: State) = (flags and (1 shl state.ordinal)) != 0
        }
    }

    /**
     * Used for state bitfield generation.
     * @param state The state to add to the bitfield.
     *
     * @return The resulting bitfield.
     */
    operator fun Int.plus(state: State) = State.combine(state, base = this)

    /**
     * Used for checking if a state bitfield contains the specified state.
     * @param state The state to check for.
     *
     * @return True if the state is set, false otherwise.
     */
    operator fun Int.contains(state: State) = State.isSet(this, state)

    /**
     * Describes which behaviour an element has.
     * NONE: The element has no behaviour.
     * ONCE_DOWN: The element is active once on touch down.
     * ONCE_UP: The element is active once on touch up.
     * REPEATED: The element is active as long as it is interacted with.
     */
    enum class TouchBehaviour {
        NONE,
        ONCE_DOWN,
        ONCE_UP,
        REPEATED
    }

    /**
     * Describes the supported plot types.
     * POINTS: The plot is a series of points.
     * BARS: The plot is a series of bars.
     * LINES: The plot is a series of lines.
     */
    enum class PlotType {
        POINTS,
        BARS,
        LINES
    }

    private val commandList = GUICommandList()
    private val transform = Matrix4x4()
    private var isSameLine = false
    private var lineHeight = 0.0f
    private var tooltipCounter = 0.0f
    private var lastTime = Time.current
    private var addToLayer = true
    private var lastUpdatedFrame = -1
    private var droppedTimeCounter = 0.0f
    private var isJustDoubleTapped = false

    private val inputListener = object : InputListener {
        override fun onKey(key: Key, down: Boolean, time: Double) {
            if (down)
                currentTextData?.onKeyAction(key)
        }

        override fun onChar(char: Char, time: Double) {
            currentTextData?.onCharAction(char)
        }

        override fun onScroll(x: Float, y: Float, time: Double) {
            currentScrollAmount.x -= x * skin.scrollSpeed
            currentScrollAmount.y -= y * skin.scrollSpeed
        }
    }

    private val gestureListener = object : GestureListener {
        override fun onTap(x: Float, y: Float, count: Int) {
            if (count == 2)
                isJustDoubleTapped = true
        }
    }

    private val dropListener: DropListener = {
        externalDroppedElements += it
        externalDropLocation.set(touchPosition)
    }

    private val matrixStack = MatrixStack()
    private val boundsPath = VectorPath()
    private val rectanglePool = ReclaimingPool(supplier = { Rectangle() }, reset = { it.infinite() })
    private val glyphLayoutPool = ReclaimingPool(supplier = { GlyphLayout() })
    private val commandListPool = ReclaimingPool(supplier = { GUICommandList() })
    private val elementPool = ReclaimingPool(supplier = { GUIElement() })
    private val popupStack = arrayListOf<GUIPopup>()
    private val layers = arrayListOf<GUILayer>()
    private var currentLayerIndex = 0
    private var isMeasuringElementSize = false
    private var isInteractionDisabledFromPopup = false
    private var internalCurrentCommandList = commandList

    /**
     * If set to false, globally disables interaction by always returning an empty bitfield on [getState].
     */
    var isInteractionEnabled = true

    /**
     * The last element that was added to the GUI.
     */
    var lastElement: GUIElement? = null
        private set

    /**
     * The current layer.
     */
    val currentLayer get() = layers[currentLayerIndex]

    /**
     * The current command list.
     */
    val currentCommandList
        get() = if (isMeasuringElementSize) GUINoopCommandList else internalCurrentCommandList

    /**
     * The current group, if one is present.
     * @see GUIGroup
     */
    var currentGroup: GUIGroup? = null
        private set

    /**
     * The current scissor rectangle, if one is present.
     */
    var currentScissorRectangle: Rectangle? = null

    /**
     * The current text data, if one is present.
     */
    var currentTextData: TextData? = null

    /**
     * The current combobox data, if one is present.
     */
    var currentComboBoxData: ComboboxData<*>? = null

    /**
     * The current drag and drop data, if one is present.
     */
    var currentDragDropData: DragDropData<*>? = null

    /**
     * The elements dropped by [DropListener].
     */
    var externalDroppedElements = arrayListOf<String>()

    /**
     * The location of the externally dropped elements.
     */
    val externalDropLocation = Vector2()

    /**
     * The current touch position.
     */
    val touchPosition = Vector2()
        get() = field.set(Kore.input.x.toFloat(), (Kore.graphics.height - Kore.input.y).toFloat())

    /**
     * The previous frames' touch position.
     */
    val lastTouchPosition = Vector2()
        get() = field.set(Kore.input.lastX.toFloat(), (Kore.graphics.height - Kore.input.lastY).toFloat())

    /**
     * The current scroll amount.
     */
    val currentScrollAmount = Vector2()

    /**
     * The font used for text rendering.
     */
    val drawableFont = SDFFont(skin.font, size = skin.contentSize)

    /**
     * Whether a tooltip should be shown, based on the time the pointer stands still.
     */
    val shouldShowTooltip get() = tooltipCounter >= skin.tooltipDelay

    /**
     * Whether [setLastElement] should consider the current scissor rectangle for calculating element positions.
     */
    var useScissorRectangleForElementPositioning = true

    /**
     * Is true when any number of popups are open.
     */
    val isPopupOpen get() = popupStack.isNotEmpty()

    init {
        Kore.input.addListener(inputListener)
        Kore.addDropListener(dropListener)

        layers.add(GUILayer())
        internalCurrentCommandList = currentLayer.commands
    }

    /**
     * Gets a pooled [Rectangle] that will automatically be freed on [end].
     */
    fun getPooledRectangle(): Rectangle = rectanglePool.obtain()

    /**
     * Gets a pooled [GlyphLayout] that will automatically be freed on [end].
     */
    fun getPooledGlyphLayout(): GlyphLayout = glyphLayoutPool.obtain()

    /**
     * Gets a pooled [GUICommandList] that will automatically be freed on [end].
     */
    fun getPooledCommandList(): GUICommandList = commandListPool.obtain()

    /**
     * Executes [block] in a new layer on top of the current one.
     * Also sets the current command list for the time [block] runs to the new layer's command list.
     * Afterwards the current command list is set back to the previous one.
     *
     * @param block The block to execute.
     */
    fun <R> layerUp(block: () -> R): R {
        currentLayerIndex++
        if (currentLayerIndex >= layers.size)
            layers.add(GUILayer())

        val previousCommandList = internalCurrentCommandList
        internalCurrentCommandList = currentLayer.commands

        val scissorRectangle = currentScissorRectangle
        if (scissorRectangle != null)
            currentCommandList.pushScissor(scissorRectangle.x, scissorRectangle.y, scissorRectangle.width, scissorRectangle.height)

        val result = block()

        if (scissorRectangle != null)
            currentCommandList.popScissor()

        internalCurrentCommandList = previousCommandList
        currentLayerIndex--

        return result
    }

    /**
     * Executes [block] in a new layer below of the current one.
     * Also sets the current command list for the time [block] runs to the new layer's command list.
     * Afterwards the current command list is set back to the previous one.
     *
     * @param block The block to execute.
     */
    fun <R> layerDown(block: () -> R): R {
        currentLayerIndex--
        if (currentLayerIndex < 0) {
            currentLayerIndex = 0
            layers.add(0, GUILayer())
        }

        val previousCommandList = internalCurrentCommandList
        internalCurrentCommandList = currentLayer.commands

        val scissorRectangle = currentScissorRectangle
        if (scissorRectangle != null)
            currentCommandList.pushScissor(scissorRectangle.x, scissorRectangle.y, scissorRectangle.width, scissorRectangle.height)

        val result = block()

        if (scissorRectangle != null)
            currentCommandList.popScissor()

        internalCurrentCommandList = previousCommandList
        currentLayerIndex++

        return result
    }

    /**
     * Executes [block] on the top layer.
     * Also sets the current command list for the time [block] runs to the top layer's command list.
     * Afterwards the current command list and current layer is set back to the previous one.
     *
     * @param block The block to execute.
     */
    fun <R> topLayer(block: () -> R): R {
        val previousLayerIndex = currentLayerIndex
        currentLayerIndex = layers.size - 1

        val previousCommandList = internalCurrentCommandList
        internalCurrentCommandList = currentLayer.commands

        val scissorRectangle = currentScissorRectangle
        if (scissorRectangle != null)
            currentCommandList.pushScissor(scissorRectangle.x, scissorRectangle.y, scissorRectangle.width, scissorRectangle.height)

        val result = block()

        if (scissorRectangle != null)
            currentCommandList.popScissor()

        internalCurrentCommandList = previousCommandList
        currentLayerIndex = previousLayerIndex

        return result
    }

    /**
     * Executes [block] on the bottom layer.
     * Also sets the current command list for the time [block] runs to the bottom layer's command list.
     * Afterwards the current command list and current layer is set back to the previous one.
     *
     * @param block The block to execute.
     */
    fun <R> bottomLayer(block: () -> R): R {
        val previousLayerIndex = currentLayerIndex
        currentLayerIndex = 0

        val previousCommandList = internalCurrentCommandList
        internalCurrentCommandList = currentLayer.commands

        val scissorRectangle = currentScissorRectangle
        if (scissorRectangle != null)
            currentCommandList.pushScissor(scissorRectangle.x, scissorRectangle.y, scissorRectangle.width, scissorRectangle.height)

        val result = block()

        if (scissorRectangle != null)
            currentCommandList.popScissor()

        internalCurrentCommandList = previousCommandList
        currentLayerIndex = previousLayerIndex

        return result
    }

    /**
     * Transforms every element added to this [GUI] while executing [block] with the given [matrix].
     * If this call is nested, a matrix stack is used so every call to this uses the previous one to calculate the complete transform matrix.
     *
     * @param matrix The matrix to transform the elements with.
     * @param block The block to execute.
     */
    fun <R> transformed(matrix: Matrix3x2, block: () -> R): R {
        currentCommandList.pushMatrix(matrix)
        matrixStack.push(matrix)

        val result = block()

        currentCommandList.popMatrix()
        matrixStack.pop()

        return result
    }

    /**
     * Records the commands added during execution of [block] and returns them as a command list.
     *
     * @param block The block to execute.
     *
     * @return The command list.
     */
    fun recordCommands(block: () -> Unit): GUICommandList {
        val list = getPooledCommandList()
        val previousCommandList = internalCurrentCommandList
        internalCurrentCommandList = list
        block()
        internalCurrentCommandList = previousCommandList
        return list
    }

    /**
     * Sets the last element.
     * This is used to determine the position of the next element.
     * This is also used to determine the size of the panel.
     *
     * All GUI functions must end with this and should return the element.
     *
     * @param x The x position of the element.
     * @param y The y position of the element.
     * @param width The width of the element.
     * @param height The height of the element.
     *
     * @return The element.
     */
    fun setLastElement(x: Float, y: Float, width: Float, height: Float): GUIElement {
        val element = elementPool.obtain()
        element.x = x
        element.y = y
        element.width = width
        element.height = height

        if (isSameLine) {
            element.getNextX = { x + width }
            element.getNextY = { y }
        } else {
            element.getNextX = { x }
            element.getNextY = { y + height }
        }

        return setLastElement(element)
    }

    /**
     * Sets the last element.
     * This is used to determine the position of the next element.
     * This is also used to determine the size of the panel.
     *
     * All GUI functions must end with this and should return the element.
     *
     * @param element The element.
     *
     * @return The element.
     */
    fun setLastElement(element: GUIElement): GUIElement {
        lastElement = element

        var elementMinX = element.x
        var elementMinY = element.y
        var elementMaxX = element.x + element.width
        var elementMaxY = element.y + element.height

        if (useScissorRectangleForElementPositioning)
            currentScissorRectangle?.let {
                val minX2 = it.minX
                val minY2 = it.minY
                val maxX2 = it.maxX
                val maxY2 = it.maxY

                if (!intersectRectRect(elementMinX, elementMinY, elementMaxX, elementMaxY, minX2, minY2, maxX2, maxY2))
                    return element

                elementMinX = max(elementMinX, minX2)
                elementMinY = max(elementMinY, minY2)
                elementMaxX = min(elementMaxX, maxX2)
                elementMaxY = min(elementMaxY, maxY2)
            }

        if (addToLayer)
            currentLayer.addElement(elementMinX, elementMinY, elementMaxX, elementMaxY)

        lineHeight = if (isSameLine)
            max(lineHeight, elementMaxY - elementMinY)
        else
            elementMaxY - elementMinY

        currentGroup?.let {
            it.width = max(it.width, elementMaxX - it.x + skin.elementPadding)

            if (!isSameLine)
                it.height += lineHeight
        }

        return element
    }

    /**
     * Gets the last element.
     * This is used to determine the position of the current element.
     *
     * @param defaultX The default x position of the element if no last element is set. Defaults to 0.0f.
     * @param defaultY The default y position of the element if no last element is set. Defaults to 0.0f.
     *
     * @return The last element.
     */
    fun getLastElement(defaultX: Float = 0.0f, defaultY: Float = 0.0f): GUIElement {
        return lastElement ?: absolute(defaultX, defaultY)
    }

    /**
     * Sets the last element to an absolute position.
     *
     * @param point The position.
     *
     * @return The created element.
     */
    fun absolute(point: Vector2) = absolute(point.x, point.y)

    /**
     * Sets the last element to an absolute position.
     *
     * @param x The x position.
     * @param y The y position.
     *
     * @return The created element.
     */
    fun absolute(x: Float, y: Float): GUIElement {
        val element = elementPool.obtain()
        element.x = x
        element.y = y
        element.width = 0.0f
        element.height = 0.0f
        element.getNextX = { element.x }
        element.getNextY = { element.y }
        return element
    }

    /**
     * Sets the last element to a relative position.
     * It's calculated by x = factorX * srcX and y = factorY * srcY.
     *
     * @param factorX The x factor, should be between 0 and 1.
     * @param factorY The y factor, should be between 0 and 1.
     * @param srcX The source size in the x dimension. Defaults to the surface width.
     * @param srcY The source size in the y dimension. Defaults to the surface height.
     *
     * @return The created element.
     */
    fun relative(factorX: Float, factorY: Float, srcX: Float = Kore.graphics.width.toFloat(), srcY: Float = Kore.graphics.height.toFloat()): GUIElement {
        val element = elementPool.obtain()
        element.x = factorX * srcX
        element.y = factorY * srcY
        element.width = 0.0f
        element.height = 0.0f
        element.getNextX = { element.x }
        element.getNextY = { element.y }
        return element
    }

    /**
     * Sets the last element to an offset from [src].
     * It's calculated by x = srcX + offsetX and y = srcY + offsetY.
     *
     * @param offset The offset.
     * @param src The source element. Defaults to the last element.
     * @param resetX Whether to reset the x position afterwards. Defaults to false.
     * @param resetY Whether to reset the y position afterwards. Defaults to false.
     * @param block The block to execute.
     */
    fun offset(offset: GUIElement, src: GUIElement = getLastElement(), resetX: Boolean = false, resetY: Boolean = false, block: () -> Unit) = offset(offset.nextX, offset.nextY, src, resetX, resetY, block)

    /**
     * Sets the last element to an offset from [src].
     * It's calculated by x = srcX + offsetX and y = srcY + offsetY.
     *
     * @param offsetX The x offset.
     * @param offsetY The y offset.
     * @param src The source element. Defaults to the last element.
     * @param resetX Whether to reset the x position afterwards. Defaults to false.
     * @param resetY Whether to reset the y position afterwards. Defaults to false.
     * @param block The block to execute.
     */
    fun offset(offsetX: Float, offsetY: Float, src: GUIElement = getLastElement(), resetX: Boolean = false, resetY: Boolean = false, block: () -> Unit) = offset(offsetX, offsetY, src.nextX, src.nextY, resetX, resetY, block)

    /**
     * Sets the last element to an offset from [src].
     * It's calculated by x = srcX + offsetX and y = srcY + offsetY.
     *
     * @param offsetX The x offset.
     * @param offsetY The y offset.
     * @param srcX The source x position.
     * @param srcY The source y position.
     * @param resetX Whether to reset the x position afterwards. Defaults to false.
     * @param resetY Whether to reset the y position afterwards. Defaults to false.
     * @param block The block to execute.
     */
    fun offset(offsetX: Float, offsetY: Float, srcX: Float, srcY: Float, resetX: Boolean = false, resetY: Boolean = false, block: () -> Unit): GUIElement {
        val lastElement = getLastElement()
        setLastElement(srcX + offsetX, srcY + offsetY, 0.0f, 0.0f)
        block()
        return setLastElement(if (resetX) lastElement.nextX else getLastElement().nextX, if (resetY) lastElement.nextY else getLastElement().nextY, 0.0f, 0.0f)
    }

    /**
     * Performs a block of code and resetting the last element afterwards.
     *
     * @param ignoreGroup If set to true, the current group, won't be affected by [block].
     * @param addToLayer If set to false, no visibility data will be added by [block].
     * @param block The block to execute.
     */
    fun transient(ignoreGroup: Boolean = false, addToLayer: Boolean = true, block: () -> Unit): GUIElement {
        val lastElement = getLastElement()
        val previousGroup = currentGroup
        val previousAddToLayer = this.addToLayer

        if (ignoreGroup)
            currentGroup = null

        if (addToLayer != this.addToLayer)
            this.addToLayer = addToLayer

        block()

        currentGroup = previousGroup
        this.addToLayer = previousAddToLayer
        this.lastElement = lastElement

        return lastElement
    }

    /**
     * Adds a blank line element.
     *
     * @param size The size of the blank line.
     */
    fun blankLine(size: Float = skin.elementSize): GUIElement {
        val (x, y) = getLastElement()
        return setLastElement(x, y, 0.0f, size)
    }

    /**
     * Adds a spacing element.
     * The spacing is a horizontal gap of the given [width].
     *
     * @param width The width of the spacing. Defaults to [skin.elementSize].
     */
    fun spacing(width: Float = skin.elementSize): GUIElement {
        val (x, y) = getLastElement()
        return setLastElement(x, y, width, 0.0f)
    }

    /**
     * Checks if [position] is visible for current layer.
     *
     * @param position The position to check.
     *
     * @return Whether the position is visible.
     */
    fun isPositionVisible(position: Vector2): Boolean {
        for (layerIndex in layers.indices.reversed()) {
            if (layerIndex == currentLayerIndex)
                break

            if (layers[layerIndex].contains(position.x, position.y))
                return false
        }

        return true
    }

    /**
     * Checks if [position] is inside the current scissor rect.
     *
     * @param position The position to check.
     *
     * @return Whether the position is inside.
     */
    fun isPositionInsideCurrentScissorRect(position: Vector2): Boolean {
        currentScissorRectangle?.let {
            if (position !in it)
                return false
        }

        return true
    }

    /**
     * Gets the state of the area covered by [rectangle] with the given [behaviour].
     *
     * @param rectangle The rectangle.
     * @param behaviour The behaviour. Defaults to [TouchBehaviour.NONE].
     * @param checkVisibility Whether to check for visibility. If set to false, other overlapping elements will be ignored.
     *
     * @return The state of the area as a bitfield.
     */
    fun getState(rectangle: Rectangle, behaviour: TouchBehaviour = TouchBehaviour.NONE, checkVisibility: Boolean = true): Int {
        if (!isInteractionEnabled)
            return 0

        if (isInteractionDisabledFromPopup)
            return 0

        if (checkVisibility && !isPositionVisible(touchPosition))
            return 0

        if (!isPositionInsideCurrentScissorRect(touchPosition))
            return 0

        var state = 0

        val isHovered = if (matrixStack.isEmpty)
            touchPosition in rectangle
        else {
            boundsPath.clear()
            matrixStack.currentMatrix.transform(rectangle.minX, rectangle.minY, boundsPath::add)
            matrixStack.currentMatrix.transform(rectangle.maxX, rectangle.minY, boundsPath::add)
            matrixStack.currentMatrix.transform(rectangle.maxX, rectangle.maxY, boundsPath::add)
            matrixStack.currentMatrix.transform(rectangle.minX, rectangle.maxY, boundsPath::add)
            touchPosition in boundsPath
        }

        if (isHovered) {
            if (currentDragDropData != null || externalDroppedElements.isNotEmpty()) {
                state += State.HOVERED_DROPPABLE

                when (behaviour) {
                    TouchBehaviour.ONCE_DOWN -> if (Kore.input.justTouchedDown) state += State.ACTIVE_DROPPABLE
                    TouchBehaviour.ONCE_UP -> if (Kore.input.justTouchedUp) state += State.ACTIVE_DROPPABLE
                    TouchBehaviour.REPEATED -> if (Kore.input.isTouched) state += State.ACTIVE_DROPPABLE
                    else -> {}
                }
            } else {
                state += State.HOVERED

                when (behaviour) {
                    TouchBehaviour.ONCE_DOWN -> if (Kore.input.justTouchedDown) state += State.ACTIVE
                    TouchBehaviour.ONCE_UP -> if (Kore.input.justTouchedUp) state += State.ACTIVE
                    TouchBehaviour.REPEATED -> if (Kore.input.isTouched) state += State.ACTIVE
                    else -> {}
                }

                if (isJustDoubleTapped)
                    state += State.DOUBLE_TAP
            }

            if (lastTouchPosition !in rectangle)
                state += State.ENTERED
        } else if (lastTouchPosition in rectangle)
            state += State.LEFT

        return state
    }

    /**
     * Marks the elements added during the execution of [block] to be laid out horizontally.
     * The elements will be laid out from left to right.
     *
     * @param block The block to execute.
     */
    fun sameLine(block: () -> Unit): GUIElement {
        if (isSameLine) {
            block()
            return getLastElement()
        }

        val lastElement = getLastElement()
        isSameLine = true
        lineHeight = 0.0f

        block()

        isSameLine = false

        return setLastElement(lastElement.nextX, lastElement.nextY, getLastElement().nextX - lastElement.nextX, lineHeight)
    }

    /**
     * Groups the elements added during the execution of [block] and sets the last element as a union of those.
     * This is useful for grouping elements that are not laid out horizontally.
     *
     * @param block The block to execute.
     */
    fun group(backgroundColor: Color? = null, minWidth: Float? = null, minHeight: Float? = null, block: () -> Unit): GUIElement {
        val (x, y) = getLastElement()

        val previousGroup = currentGroup
        val previousSameLine = isSameLine
        val previousLineHeight = lineHeight

        val group = GUIGroup(x, y, 0.0f, 0.0f)
        currentGroup = group
        isSameLine = false
        lineHeight = 0.0f

        val commands = recordCommands(block)

        val groupX = group.x
        val groupY = group.y
        var groupWidth = group.width
        var groupHeight = group.height

        minWidth?.let { groupWidth = max(groupWidth, it) }
        minHeight?.let { groupHeight = max(groupHeight, it) }

        backgroundColor?.let {
            currentCommandList.drawRectFilled(groupX, groupY, groupWidth, groupHeight, skin.roundedCorners, skin.cornerRounding, it)
        }
        currentCommandList.addCommandList(commands)

        currentGroup = previousGroup
        isSameLine = previousSameLine
        lineHeight = previousLineHeight

        return setLastElement(groupX, groupY, groupWidth, groupHeight)
    }

    /**
     * @see [popup]
     */
    fun popup(block: GUIPopup.(GUI, Float, Float) -> GUIElement) = popup(object : GUIPopup() {
        override fun draw(gui: GUI, width: Float, height: Float) = block(this, gui, width, height)
    })

    /**
     * Adds [popup] to the top of this gui's popup stack.
     * Only the upmost popup of this stack is interactable.
     */
    fun popup(popup: GUIPopup) {
        popup.isActive = true
        popupStack += popup
    }

    /**
     * Returns an [GUIElement] instance containing the size of the element returned by [block].
     *
     * @param block The block to execute to retrieve the element.
     */
    fun getElementSize(block: () -> Any): GUIElement {
        val previousIsInteractionEnabled = isInteractionEnabled
        isInteractionEnabled = false

        lateinit var result: GUIElement

        val previousIsMeasuringElementSize = isMeasuringElementSize
        isMeasuringElementSize = true

        transient(true, false) {
            result = group { block() }
        }

        isMeasuringElementSize = previousIsMeasuringElementSize

        isInteractionEnabled = previousIsInteractionEnabled

        return result
    }

    /**
     * Begin GUI rendering.
     * This must be called before any widget function.
     */
    fun begin() {
        lastElement = null

        val currentTime = Time.current
        val deltaTime = (currentTime - lastTime).toFloat()
        lastTime = currentTime

        if (touchPosition.x == lastTouchPosition.x && touchPosition.y == lastTouchPosition.y)
            tooltipCounter += deltaTime
        else
            tooltipCounter = 0.0f

        currentDragDropData?.isRendered = false

        isInteractionDisabledFromPopup = popupStack.isNotEmpty()
    }

    /**
     * Returns an [GUIVisibility] instance that contains every element that's been added until the invocation of this method.
     * This can be used to check if a point is outside any GUI elements.
     *
     * @return The computed [GUIVisibility].
     */
    fun getCompleteVisibility(): GUIVisibility {
        val visibility = GUIVisibility()
        layers.forEach {
            it.addToVisibility(visibility)
        }
        return visibility
    }

    /**
     * End GUI rendering.
     * This must be called after all widget functions.
     * It will render the GUI by flushing the command list.
     * It will also reset the command list.
     */
    fun end() {
        if (lastUpdatedFrame != Kore.graphics.statistics.numFrames) {
            isJustDoubleTapped = false
            currentScrollAmount.mul(0.75f)

            if (currentScrollAmount.lengthSquared <= 0.01f)
                currentScrollAmount.setZero()

            if (Kore.input.justTouchedUp)
                Kore.onNextFrame {
                    currentDragDropData = null
                }

            if (externalDroppedElements.isNotEmpty()) {
                droppedTimeCounter += Kore.graphics.statistics.frameTime

                if (droppedTimeCounter >= 1.0f)
                    externalDroppedElements.clear()
            } else
                droppedTimeCounter = 0.0f

            lastUpdatedFrame = Kore.graphics.statistics.numFrames
        }

        fun drawPopup(index: Int) {
            val popup = popupStack.getOrNull(index) ?: return

            transient {
                setLastElement(absolute(0.0f, 0.0f))

                val size = getElementSize {
                    popup.draw(this, 0.0f, 0.0f)
                }

                setLastElement(absolute((Kore.graphics.safeWidth - size.width) * 0.5f, (Kore.graphics.safeHeight - size.height) * 0.5f))

                if (index == popupStack.lastIndex)
                    isInteractionDisabledFromPopup = false

                popup.draw(this, size.width, size.height)

                if (index == popupStack.lastIndex)
                    isInteractionDisabledFromPopup = true
            }

            if (!popup.isActive) {
                popupStack.removeAt(index)
                drawPopup(index)
            } else
                drawPopup(index + 1)
        }

        if (popupStack.isNotEmpty())
            topLayer {
                drawPopup(0)
            }

        currentDragDropData?.let {
            topLayer {
                it.drawPayload(this)
            }
        }

        transform.setToOrtho2D(Kore.graphics.safeInsetLeft.toFloat(), Kore.graphics.safeWidth.toFloat(), Kore.graphics.safeHeight.toFloat(), Kore.graphics.safeInsetTop.toFloat())

        Game.graphics2d.render(transform) { renderer ->
            renderer.withTransientState {
                flipX = false
                flipY = true
                shader = DefaultShader
                layers.forEach {
                    it.process(renderer)
                }
            }
        }

        rectanglePool.freePooled()
        glyphLayoutPool.freePooled()
        commandListPool.freePooled()
        elementPool.freePooled()
    }

    /**
     * Disposes the GUI and all its resources.
     */
    override fun dispose() {
        Kore.input.removeListener(inputListener)
        Kore.removeDropListener(dropListener)
        drawableFont.dispose()
    }
}
