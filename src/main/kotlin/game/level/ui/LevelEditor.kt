package game.level.ui

import com.cozmicgames.Kore
import com.cozmicgames.graphics
import com.cozmicgames.input
import com.cozmicgames.input.Keys
import com.cozmicgames.input.MouseButtons
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.collections.Array2D
import com.cozmicgames.utils.maths.OrthographicCamera
import com.cozmicgames.utils.maths.Vector2
import com.cozmicgames.utils.maths.unproject
import engine.Game
import engine.graphics.drawPathStroke
import engine.graphics.drawRect
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIVisibility
import engine.graphics.ui.TextData
import engine.graphics.ui.widgets.*
import engine.scene.Scene
import engine.scene.components.TransformComponent
import engine.scene.findGameObjectByComponent
import engine.scene.findGameObjectsWithComponent
import engine.assets.managers.getMaterial
import engine.assets.managers.getTexture
import engine.assets.managers.getTileSet
import game.assets.AssetSelectorData
import game.assets.assetSelector
import game.assets.types.TileSetAssetType
import game.components.FreeCameraControllerComponent
import game.components.CameraComponent
import game.components.GridComponent
import game.components.getCellType
import game.extensions.*
import game.level.TileSet
import kotlin.math.ceil
import kotlin.math.floor

class LevelEditor(val scene: Scene) : Disposable {
    sealed interface ReturnState {
        object None : ReturnState

        object Menu : ReturnState

        object MainMenu : ReturnState
    }

    private enum class ToolType(val texture: String) {
        PENCIL("internal/images/pencil_tool.png"),
        DELETE("internal/images/delete_tool.png"),
        SELECT("internal/images/select_tool.png"),
        PICK("internal/images/pick_tool.png"),
        FILL("internal/images/fill_tool.png"),
        UNDO("internal/images/undo_tool.png"),
        REDO("internal/images/redo_tool.png"),
        COPY("internal/images/copy_tool.png"),
        PASTE("internal/images/paste_tool.png"),
        SETTINGS("internal/images/settings.png")
    }

    private val gui = GUI()
    private val commandExecutor = EditorCommandExecutor()
    private val tilesScroll = Vector2()
    private val assetSelectorData = AssetSelectorData()

    private var copiedRegion: Array2D<String?>? = null
    private var selectionRegion: GridRegion? = null
    private var currentTool = ToolType.PENCIL
    private var currentType = ""
    private var currentGridLayer = findGridLayerUp(-Int.MAX_VALUE)
    private val visibleGridLayers = arrayListOf<Int>()
    private var isLayerSelectionOpen = false
    private val layerCellSizeTextData = TextData {
        text.toFloatOrNull()?.let {
            val gridObject = scene.findGameObjectByComponent<GridComponent> { it.getComponent<GridComponent>()?.layer == currentGridLayer }
            gridObject?.getComponent<GridComponent>()?.cellSize = it
        }
    }

    private var isInteractionEnabled = false
        set(value) {
            if (field == value)
                return

            field = value

            if (value) {
                Game.controls.add("freecamera_move").also {
                    it.addMouseButton(MouseButtons.MIDDLE)
                }
                Game.controls.add("freecamera_move_x").also {
                    it.setDeltaX()
                }
                Game.controls.add("freecamera_move_y").also {
                    it.setDeltaY()
                }
                Game.controls.add("freecamera_zoom").also {
                    it.setScrollY()
                }
            } else {
                Game.controls.remove("freecamera_move")
                Game.controls.remove("freecamera_move_x")
                Game.controls.remove("freecamera_move_y")
                Game.controls.remove("freecamera_zoom")
            }
        }

    init {
        assetSelectorData.showEditIcons = true
        assetSelectorData.filter = listOf(TileSet::class)
    }

    private fun findGridLayerUp(layer: Int): Int? {
        val gridComponents = arrayListOf<GridComponent>()

        scene.findGameObjectsWithComponent<GridComponent> {
            it.getComponent<GridComponent>()?.let {
                gridComponents += it
            }
        }

        var closestGridComponent: GridComponent? = null

        for (gridComponent in gridComponents) {
            if (gridComponent.layer <= layer)
                continue

            if (closestGridComponent == null)
                closestGridComponent = gridComponent
            else if (closestGridComponent.layer > gridComponent.layer)
                closestGridComponent = gridComponent
        }

        return closestGridComponent?.layer
    }

    private fun findGridLayerDown(layer: Int): Int? {
        val gridComponents = arrayListOf<GridComponent>()

        scene.findGameObjectsWithComponent<GridComponent> {
            it.getComponent<GridComponent>()?.let {
                gridComponents += it
            }
        }

        var closestGridComponent: GridComponent? = null

        for (gridComponent in gridComponents) {
            if (gridComponent.layer >= layer)
                continue

            if (closestGridComponent == null)
                closestGridComponent = gridComponent
            else if (closestGridComponent.layer < gridComponent.layer)
                closestGridComponent = gridComponent
        }

        return closestGridComponent?.layer
    }

    fun enableInteraction() {
        gui.isInteractionEnabled = true
    }

    fun disableInteraction() {
        gui.isInteractionEnabled = false
    }

    private fun copySelection() {
        selectionRegion?.let {
            copiedRegion = it.getTiles()
        }
    }

    private fun pasteSelection() {
        selectionRegion?.let {
            copiedRegion?.let { source ->
                commandExecutor.setTileTypes(it, source)
            }
        }
    }

    private fun deleteSelection() {
        selectionRegion?.let {
            it.setTiles { _, _ -> null }
        }
    }

    private fun drawBackground(grid: GridComponent, camera: OrthographicCamera) {
        val backgroundTexture = Game.assets.getTexture("internal/images/grid_background_8x8.png")

        val backgroundTileWidth = 8 * grid.cellSize
        val backgroundTileHeight = 8 * grid.cellSize

        val numBackgroundTilesX = ceil(camera.rectangle.width / backgroundTileWidth).toInt() + 1
        val numBackgroundTilesY = ceil(camera.rectangle.height / backgroundTileHeight).toInt() + 1

        var backgroundTileX = floor((camera.position.x - camera.rectangle.width * 0.5f) / backgroundTileWidth) * backgroundTileWidth

        repeat(numBackgroundTilesX) {
            var backgroundTileY = floor((camera.position.y - camera.rectangle.height * 0.5f) / backgroundTileHeight) * backgroundTileHeight

            repeat(numBackgroundTilesY) {
                Game.renderer.submit(grid.layer - 1, backgroundTexture.texture, "default", false, false) {
                    it.drawRect(backgroundTileX, backgroundTileY, backgroundTileWidth, backgroundTileHeight, color = Color.LIGHT_GRAY, u0 = backgroundTexture.u0, v0 = backgroundTexture.v0, u1 = backgroundTexture.u1, v1 = backgroundTexture.v1)
                }

                backgroundTileY += backgroundTileHeight
            }

            backgroundTileX += backgroundTileWidth
        }
    }

    fun drawTitle(setReturnState: (ReturnState) -> Unit) {
        val label = {
            gui.label("Edit Level", null)
        }

        val settingsButton = {
            gui.textButton("Settings") {
                setReturnState(ReturnState.Menu)
            }
        }

        val backButton = {
            gui.textButton("Back") {
                setReturnState(ReturnState.MainMenu)
            }
        }

        val saveButton = {
            gui.textButton("Save") {
                //TODO: Save
                setReturnState(ReturnState.MainMenu)
            }
        }

        val labelWidth = gui.getElementSize(label).width

        val buttonsWidth = gui.getElementSize {
            gui.sameLine {
                settingsButton()
                backButton()
                saveButton()
            }
        }.width

        gui.group(Game.editorStyle.panelTitleBackgroundColor) {
            gui.sameLine {
                label()
                gui.spacing(Kore.graphics.width - labelWidth - buttonsWidth)
                settingsButton()
                backButton()
                saveButton()
            }
        }
    }

    fun drawCurrentTool(grid: GridComponent, camera: OrthographicCamera, visibility: GUIVisibility) {
        val (worldX, worldY, _) = camera.unproject(Kore.input.x.toFloat(), Kore.input.y.toFloat())

        val tileX = floor(worldX / grid.cellSize).toInt()
        val tileY = floor(worldY / grid.cellSize).toInt()

        val selectionRegion = this.selectionRegion

        if (selectionRegion != null) {
            Game.renderer.submit(grid.layer - 1, Game.graphics2d.blankTexture, "default", false, false) {
                val x = selectionRegion.minX * grid.cellSize
                val y = selectionRegion.minY * grid.cellSize
                val width = selectionRegion.width * grid.cellSize
                val height = selectionRegion.height * grid.cellSize

                it.drawPathStroke(it.path {
                    rect(x, y, width, height)
                }, 2.0f, true, Color.WHITE)
            }
        }

        if (Vector2(Kore.input.x.toFloat(), Kore.graphics.height - Kore.input.y.toFloat()) in visibility)
            return

        when (currentTool) {
            ToolType.PENCIL -> {
                if (isInteractionEnabled && Game.assets.getTileSet(grid.tileSet)?.contains(currentType) == true) {
                    val previewMaterial = Game.assets.getMaterial(Game.assets.getTileSet(grid.tileSet)?.let {
                        it[currentType]?.getMaterial(grid, tileX, tileY)
                    } ?: "<missing>") ?: Game.graphics2d.missingMaterial

                    val previewTexture = Game.assets.getTexture(previewMaterial.colorTexturePath)
                    Game.renderer.submit(grid.layer - 1, previewTexture.texture, previewMaterial.shader, false, false) {
                        it.drawRect(tileX * grid.cellSize, tileY * grid.cellSize, grid.cellSize, grid.cellSize, Color(1.0f, 1.0f, 1.0f, 0.5f), u0 = previewTexture.u0, v0 = previewTexture.v1, u1 = previewTexture.u1, v1 = previewTexture.v0)
                    }

                    if (Kore.input.isButtonDown(MouseButtons.LEFT) && grid.getCellType(tileX, tileY) != currentType)
                        commandExecutor.setTileType(grid, tileX, tileY, currentType)

                    if (Kore.input.isButtonDown(MouseButtons.RIGHT) && grid.getCellType(tileX, tileY) != null)
                        commandExecutor.setTileType(grid, tileX, tileY, null)
                }
            }
            ToolType.DELETE -> {
                if (isInteractionEnabled) {
                    if ((Kore.input.isButtonDown(MouseButtons.LEFT) || Kore.input.isButtonDown(MouseButtons.RIGHT)) && grid.getCellType(tileX, tileY) != null)
                        commandExecutor.setTileType(grid, tileX, tileY, null)
                }
            }
            ToolType.SELECT -> {
                if (isInteractionEnabled) {
                    if (Kore.input.isButtonJustDown(MouseButtons.RIGHT))
                        this.selectionRegion = null

                    if (selectionRegion == null) {
                        Game.renderer.submit(grid.layer - 1, Game.graphics2d.blankTexture, "default", false, false) {
                            it.drawPathStroke(it.path {
                                rect(tileX * grid.cellSize, tileY * grid.cellSize, grid.cellSize, grid.cellSize)
                            }, 2.0f, true, Color(1.0f, 1.0f, 1.0f, 0.5f))
                        }
                    }

                    if (Kore.input.isButtonJustDown(MouseButtons.LEFT))
                        this.selectionRegion = GridRegion(grid, tileX, tileY, tileX, tileY)

                    if (Kore.input.isButtonDown(MouseButtons.LEFT)) {
                        this.selectionRegion?.x1 = tileX
                        this.selectionRegion?.y1 = tileY
                    }
                }
            }
            ToolType.PICK -> {
                if (isInteractionEnabled) {
                    if (Kore.input.isButtonJustDown(MouseButtons.LEFT)) {
                        val type = grid.getCellType(tileX, tileY)
                        if (type != null)
                            currentType = type
                    }
                }
            }
            else -> {}
        }
    }

    fun drawToolSelection(setReturnState: (ReturnState) -> Unit) {
        gui.setLastElement(gui.absolute(Kore.graphics.width - Game.editorStyle.toolImageSize, gui.skin.elementSize))

        val titleLabel = {
            gui.label("Tools", null)
        }

        val panelWidth = gui.getElementSize(titleLabel).width
        val panelHeight = if (isLayerSelectionOpen)
            Kore.graphics.height - gui.skin.elementSize - Kore.graphics.height * Game.editorStyle.assetSelectorHeight
        else
            Kore.graphics.height - gui.skin.elementSize * 2.0f

        gui.panel(panelWidth, panelHeight, tilesScroll, Game.editorStyle.panelContentBackgroundColor, Game.editorStyle.panelTitleBackgroundColor, {
            titleLabel()
        }) {
            val imageSize = if (it.hasVerticalScrollbar) panelWidth - gui.skin.scrollbarSize else panelWidth - gui.skin.elementPadding

            ToolType.values().forEach {
                val texture = Game.assets.getTexture(it.texture)
                when (it) {
                    ToolType.FILL -> gui.imageButton(texture, imageSize) {
                        selectionRegion?.let {
                            commandExecutor.setTileTypes(it, currentType)
                        }
                    }
                    ToolType.UNDO -> gui.imageButton(texture, imageSize) {
                        commandExecutor.undo()
                    }
                    ToolType.REDO -> gui.imageButton(texture, imageSize) {
                        commandExecutor.redo()
                    }
                    ToolType.COPY -> gui.imageButton(texture, imageSize) {
                        copySelection()
                    }
                    ToolType.PASTE -> gui.imageButton(texture, imageSize) {
                        pasteSelection()
                    }
                    ToolType.SETTINGS -> gui.imageButton(texture, imageSize) {
                        setReturnState(ReturnState.Menu)
                    }
                    else -> gui.selectableImage(texture, imageSize, isSelected = currentTool == it) {
                        selectionRegion = null
                        currentTool = it
                    }
                }
            }
        }
    }

    fun drawLayerSelection() {
        gui.setLastElement(gui.absolute(0.0f, gui.skin.elementSize))

        gui.dropdown("Layers", isLayerSelectionOpen) {
            isLayerSelectionOpen = it
        }

        val gridLayers = arrayListOf<GridComponent>()

        var layer = findGridLayerDown(Int.MAX_VALUE)
        while (layer != null) {
            val gridObject = scene.findGameObjectByComponent<GridComponent> { it.getComponent<GridComponent>()?.layer == layer } ?: continue
            val gridComponent = gridObject.getComponent<GridComponent>() ?: continue
            gridLayers += gridComponent

            layer = findGridLayerDown(layer)
        }

        gridLayers.forEach {
            it.gameObject.isActive = it.layer in visibleGridLayers
        }

        if (isLayerSelectionOpen) {
            gui.layerUp {
                gui.group(Game.editorStyle.panelContentBackgroundColor) {
                    gridLayers.forEachIndexed { index, grid ->
                        gui.sameLine {
                            gui.group {
                                val wasSelectedLayer = currentGridLayer == grid.layer

                                if (index > 0)
                                    gui.upButton(gui.skin.elementSize * 0.5f) {
                                        val layerUp = findGridLayerUp(grid.layer)

                                        if (layerUp == null)
                                            grid.layer++
                                        else {
                                            val upObject = scene.findGameObjectByComponent<GridComponent> { it.getComponent<GridComponent>()?.layer == layerUp }
                                            upObject?.getComponent<GridComponent>()?.layer = grid.layer
                                            grid.layer = layerUp
                                        }
                                    }

                                if (index < gridLayers.lastIndex) {
                                    if (index == 0)
                                        gui.blankLine(gui.skin.elementSize * 0.5f)

                                    gui.downButton(gui.skin.elementSize * 0.5f) {
                                        val layerDown = findGridLayerDown(grid.layer)

                                        if (layerDown == null)
                                            grid.layer--
                                        else {
                                            val upObject = scene.findGameObjectByComponent<GridComponent> { it.getComponent<GridComponent>()?.layer == layerDown }
                                            upObject?.getComponent<GridComponent>()?.layer = grid.layer
                                            grid.layer = layerDown
                                        }
                                    }
                                }

                                if (wasSelectedLayer)
                                    currentGridLayer = grid.layer
                            }

                            val isVisible = grid.layer in visibleGridLayers
                            gui.layerVisibleButton(isVisible) {
                                if (isVisible)
                                    visibleGridLayers -= grid.layer
                                else
                                    visibleGridLayers += grid.layer
                            }

                            gui.blankLine(gui.skin.elementPadding)

                            gui.selectable({
                                gui.label("Layer $index", null)
                            }, currentGridLayer == grid.layer) {
                                if (currentGridLayer != grid.layer)
                                    layerCellSizeTextData.setText(grid.cellSize.toString())

                                currentGridLayer = grid.layer
                            }

                            gui.blankLine(gui.skin.elementPadding)

                            gui.tooltip(gui.imageButton(Game.assets.getTexture("internal/images/layer_delete.png"), color = Color.SCARLET) {
                                scene.removeGameObject(grid.gameObject)
                            }, "Delete this layer")
                        }

                        if (grid.layer == currentGridLayer)
                            gui.offset(gui.skin.elementSize * 2.0f, 0.0f, resetX = true) {
                                gui.bordered(Game.editorStyle.layerEditorBorderColor, 2.5f) {
                                    gui.sameLine {
                                        gui.group {
                                            gui.label("Tileset", null)
                                            gui.label("Is collidable", null)
                                            gui.label("Cell size", null)
                                        }
                                        gui.group {
                                            gui.droppable<TileSetAssetType.TileSetAsset>({
                                                grid.tileSet = it.name
                                            }, 2.5f) {
                                                gui.tooltip(gui.label(grid.tileSet, null, maxWidth = gui.skin.contentSize * 20.0f), grid.tileSet)
                                            }
                                            gui.checkBox(grid.isCollidable) {
                                                grid.isCollidable = it
                                            }
                                            gui.textField(layerCellSizeTextData)
                                        }
                                    }
                                }
                            }
                    }

                    gui.textButton("Create new layer") {
                        scene.addGameObject {
                            addComponent<TransformComponent> { }
                            val grid = addComponent<GridComponent> {
                                this.layer = findGridLayerUp(-Int.MAX_VALUE)?.let { it - 1 } ?: 0 //TODO: Maybe enable moving the "default" layer
                            }

                            visibleGridLayers += grid.layer

                            if (gridLayers.isEmpty())
                                layerCellSizeTextData.setText(grid.cellSize.toString())
                        }
                    }
                }

                gui.transient {
                    val assetSelectorWidth = Kore.graphics.width.toFloat()
                    val assetSelectorHeight = Kore.graphics.height * Game.editorStyle.assetSelectorHeight

                    gui.setLastElement(gui.absolute(0.0f, Kore.graphics.height - assetSelectorHeight))
                    gui.assetSelector(assetSelectorData, assetSelectorWidth, assetSelectorHeight)
                }
            }
        }
    }

    fun drawCoordinateInfoLine(grid: GridComponent, camera: OrthographicCamera) {
        gui.setLastElement(gui.absolute(0.0f, Kore.graphics.height - gui.skin.elementSize))
        gui.group(Game.editorStyle.panelContentBackgroundColor) {
            gui.sameLine {
                val (worldX, worldY, _) = camera.unproject(Kore.input.x.toFloat(), Kore.input.y.toFloat())

                val tileX = floor(worldX / grid.cellSize).toInt()
                val tileY = floor(worldY / grid.cellSize).toInt()

                gui.label("Cursor: $tileX, $tileY", null)

                selectionRegion?.let {
                    gui.spacing()
                    gui.label("Selected: ${it.width} x ${it.height}", null)
                }

                copiedRegion?.let {
                    gui.spacing()
                    gui.label("Copied: ${it.width} x ${it.height}", null)
                }
            }
        }
    }

    fun drawTypeSelection(grid: GridComponent) {
        gui.setLastElement(gui.absolute(0.0f, gui.skin.elementSize * 2.0f))

        val titleLabel = {
            gui.label("Tile types", null)
        }

        val panelWidth = gui.getElementSize(titleLabel).width
        val panelHeight = Kore.graphics.height - gui.skin.elementSize - Kore.graphics.height * Game.editorStyle.assetSelectorHeight

        gui.panel(panelWidth, panelHeight, tilesScroll, Game.editorStyle.panelContentBackgroundColor, Game.editorStyle.panelTitleBackgroundColor, {
            titleLabel()
        }) {
            val imageSize = panelWidth - gui.skin.scrollbarSize

            Game.assets.getTileSet(grid.tileSet)?.let {
                for (name in it.tileTypeNames) {
                    val tileType = it[name] ?: continue
                    val isSelected = currentType == name
                    val previewMaterial = Game.assets.getMaterial(tileType.defaultMaterial) ?: Game.graphics2d.missingMaterial

                    gui.selectable({
                        gui.materialPreview(previewMaterial, imageSize, imageSize)
                    }, isSelected) {
                        currentType = name
                    }
                }
            }
        }
    }

    fun onFrame(delta: Float): ReturnState {
        if (isInteractionEnabled) {
            if (Kore.input.isKeyDown(Keys.KEY_CONTROL) && Kore.input.isKeyJustDown(Keys.KEY_Z))
                commandExecutor.undo()

            if (Kore.input.isKeyDown(Keys.KEY_CONTROL) && Kore.input.isKeyJustDown(Keys.KEY_Y))
                commandExecutor.redo()

            if (Kore.input.isKeyDown(Keys.KEY_CONTROL) && Kore.input.isKeyJustDown(Keys.KEY_C))
                copySelection()

            if (Kore.input.isKeyDown(Keys.KEY_CONTROL) && Kore.input.isKeyJustDown(Keys.KEY_V))
                pasteSelection()

            if (Kore.input.isKeyJustDown(Keys.KEY_DELETE))
                deleteSelection()
        }

        isInteractionEnabled = gui.isInteractionEnabled && !gui.isPopupOpen

        scene.update(delta)

        if (currentGridLayer == null)
            currentGridLayer = findGridLayerUp(-Int.MAX_VALUE)

        val gridObject = scene.findGameObjectByComponent<GridComponent> { it.getComponent<GridComponent>()?.layer == currentGridLayer }

        var mainCameraComponent: CameraComponent? = null

        for (gameObject in scene.activeGameObjects) {
            val cameraComponent = gameObject.getComponent<CameraComponent>() ?: continue

            if (cameraComponent.isMainCamera) {
                mainCameraComponent = cameraComponent
                break
            }
        }

        mainCameraComponent ?: return ReturnState.Menu

        mainCameraComponent.gameObject.getComponent<FreeCameraControllerComponent>()?.isEnabled = isInteractionEnabled

        val currentGrid = gridObject?.getComponent<GridComponent>()
        val mainCamera = mainCameraComponent.camera

        var returnState: ReturnState = ReturnState.None

        if (currentGrid == null)
            currentType = ""
        else if (currentType == "")
            currentType = Game.assets.getTileSet(currentGrid.tileSet)?.tileTypeNames?.firstOrNull() ?: ""

        currentGrid?.let {
            drawBackground(it, mainCamera)
        }

        Game.renderer.render(mainCamera) { it !in mainCameraComponent.excludedLayers }
        Game.renderer.clear()

        gui.begin()
        drawTitle { returnState = it }
        drawToolSelection { returnState = it }
        drawLayerSelection()

        currentGrid?.let {
            drawTypeSelection(currentGrid)
            drawCoordinateInfoLine(currentGrid, mainCamera)
        }

        val visibility = gui.getCompleteVisibility()
        gui.end()

        currentGrid?.let {
            drawCurrentTool(currentGrid, mainCamera, visibility)
        }

        if (returnState == ReturnState.MainMenu)
            currentType = ""

        return returnState
    }

    override fun dispose() {
        gui.dispose()
    }
}