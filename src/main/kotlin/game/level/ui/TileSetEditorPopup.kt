package game.level.ui

import com.cozmicgames.Kore
import com.cozmicgames.files.writeString
import com.cozmicgames.graphics
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.Properties
import com.cozmicgames.utils.maths.Vector2
import engine.Game
import engine.graphics.ui.DragDropData
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.GUIPopup
import engine.graphics.ui.widgets.*
import engine.assets.getAssetFileHandle
import engine.assets.managers.getMaterial
import engine.assets.managers.getTileSet
import engine.graphics.TextureRegion
import game.assets.AssetSelectorData
import game.assets.assetSelector
import game.assets.types.MaterialAssetType
import game.extensions.*
import game.level.TileSet
import kotlin.math.min

class TileSetEditorPopup : GUIPopup() {
    private class TileTypeData(val name: String)
    private inner class TileTypeDragDropData(name: String) : DragDropData<TileTypeData>(TileTypeData(name), {
        materialPreview(Game.assets.getMaterial(tempTileSet[name]?.getMaterial() ?: "<mising>") ?: Game.graphics2d.missingMaterial, Game.editorStyle.toolImageSize)
    })

    private val assetSelectorData = AssetSelectorData()
    private val materialEditorData = MaterialEditorData()

    private val materialEditorScroll = Vector2()
    private var currentMaterial: String? = null
    private val tilesScroll = Vector2()
    private var currentTileType: String? = null
    private val tileTypeEditorScroll = Vector2()
    private var currentRuleIndex: Int? = null
    private var openRuleEditorIndex: Int? = null
    private val ruleEditorScrollAmounts = Array(8) { Vector2() }
    private val ruleGeneratorPopup = RuleGeneratorPopup()

    private val tempTileSet = TileSet("temp")
    private var tileSetName: String? = null

    init {
        assetSelectorData.showEditIcons = true
        assetSelectorData.filter = listOf(TextureRegion::class)
    }

    fun reset(tileSetName: String) {
        tempTileSet.clear()
        Game.assets.getTileSet(tileSetName)?.let {
            tempTileSet.set(it)
        }
        this.tileSetName = tileSetName
    }

    override fun draw(gui: GUI, width: Float, height: Float): GUIElement {
        fun drawBackground() {
            gui.transient(addToLayer = false) {
                gui.colorRectangle(Color(0.5f, 0.5f, 0.5f, 0.25f), Kore.graphics.width.toFloat(), Kore.graphics.height.toFloat())
            }
        }

        fun drawTitle() {
            val label = {
                gui.label("Edit Tileset", null)
            }

            val cancelButton = {
                gui.textButton("Cancel") {
                    closePopup()
                }
            }

            val saveButton = {
                gui.textButton("Save") {
                    tileSetName?.let {
                        val tileSet = Game.assets.getTileSet(it)
                        tileSet?.clear()
                        tileSet?.set(tempTileSet)
                        Game.assets.getAssetFileHandle(it)?.writeString(Properties().also { tileSet?.write(it) }.write(), false)
                    }
                    closePopup()
                }
            }

            val labelWidth = gui.getElementSize(label).width

            val buttonsWidth = gui.getElementSize {
                gui.sameLine {
                    cancelButton()
                    saveButton()
                }
            }.width

            gui.group(Game.editorStyle.panelTitleBackgroundColor) {
                gui.sameLine {
                    label()
                    gui.spacing(Kore.graphics.width - labelWidth - buttonsWidth)
                    cancelButton()
                    saveButton()
                }
            }
        }

        fun drawMaterialEditor(materialName: String): Float {
            val materialEditorSize = gui.getElementSize {
                gui.materialEditor(materialName, materialEditorData)
            }

            val panelWidth = materialEditorSize.width + gui.skin.scrollbarSize
            val panelHeight = min(Kore.graphics.height - gui.skin.elementSize - Kore.graphics.height * Game.editorStyle.assetSelectorHeight, materialEditorSize.height)

            gui.setLastElement(gui.absolute(Kore.graphics.width - panelWidth, gui.skin.elementSize))
            gui.panel(panelWidth, panelHeight, materialEditorScroll, Game.editorStyle.panelContentBackgroundColor, Game.editorStyle.panelTitleBackgroundColor, {
                gui.label("Material Editor", null)
            }) {
                gui.materialEditor(materialName, materialEditorData)
            }

            return materialEditorSize.width
        }

        fun drawTileList() {
            val titleLabel = {
                gui.label("Tile types")
            }

            val panelWidth = gui.getElementSize(titleLabel).width
            val panelHeight = Kore.graphics.height - gui.skin.elementSize - Kore.graphics.height * Game.editorStyle.assetSelectorHeight

            gui.panel(panelWidth, panelHeight, tilesScroll, Game.editorStyle.panelContentBackgroundColor, Game.editorStyle.panelTitleBackgroundColor, {
                titleLabel()
            }) {
                val imageSize = panelWidth - gui.skin.scrollbarSize

                for (name in tempTileSet.tileTypeNames) {
                    if (name !in tempTileSet)
                        continue

                    val tileType = tempTileSet[name] ?: continue

                    gui.elementMenu({
                        val previewMaterial = Game.assets.getMaterial(tileType.defaultMaterial) ?: Game.graphics2d.missingMaterial

                        gui.draggable({
                            TileTypeDragDropData((name))
                        }) {
                            gui.materialPreview(previewMaterial, imageSize, imageSize)
                        }
                    }, imageSize * 0.25f, arrayOf(MENUOPTION_EDIT), backgroundColor = Color.DARK_GRAY) {
                        if (it == MENUOPTION_EDIT) {
                            currentTileType = name
                            currentRuleIndex = null
                            ruleEditorScrollAmounts.forEach {
                                it.setZero()
                            }
                            currentMaterial = tempTileSet[name]?.defaultMaterial
                        }
                    }
                }

                gui.plusButton(imageSize) {
                    tempTileSet.addType()
                }
            }
        }

        fun drawTileTypeEditor() {
            if (currentTileType == null)
                return

            val tileType = tempTileSet[requireNotNull(currentTileType)] ?: return

            gui.getElementSize {
                gui.label("Tile types")
            }.width

            val panelWidth = gui.getElementSize { gui.label("Tile types", null) }.width
            val panelHeight = Kore.graphics.height - gui.skin.elementSize - Kore.graphics.height * Game.editorStyle.assetSelectorHeight

            gui.panel(panelWidth, panelHeight, tileTypeEditorScroll, Game.editorStyle.panelContentBackgroundColor, Game.editorStyle.panelTitleBackgroundColor) {
                gui.textButton("Delete", overrideFontColor = Color.SCARLET) {
                    currentTileType?.let {
                        tempTileSet.remove(it)
                        currentTileType = null
                    }
                }

                val imageSize = panelWidth - gui.skin.scrollbarSize

                gui.label("Default", Game.editorStyle.panelTitleBackgroundColor, minWidth = panelWidth)

                val defaultMaterial = Game.assets.getMaterial(tileType.defaultMaterial) ?: Game.graphics2d.missingMaterial

                gui.selectable({
                    gui.droppable<MaterialAssetType.MaterialAsset>({
                        tileType.defaultMaterial = it.name
                    }) {
                        gui.materialPreview(defaultMaterial, imageSize)
                    }
                }, currentMaterial == tileType.defaultMaterial) {
                    currentMaterial = tileType.defaultMaterial
                }

                gui.separator(panelWidth)

                gui.label("Rules", Game.editorStyle.panelTitleBackgroundColor, minWidth = panelWidth)

                tileType.rules.forEachIndexed { index, rule ->
                    val isSelected = currentRuleIndex == index

                    val material = Game.assets.getMaterial(rule.material) ?: Game.graphics2d.missingMaterial

                    gui.elementMenu({
                        gui.selectable({
                            gui.materialPreview(material, imageSize)
                        }, isSelected) {
                            currentRuleIndex = index
                            currentMaterial = rule.material
                        }
                    }, imageSize * 0.25f, arrayOf(MENUOPTION_DELETE), backgroundColor = Color.DARK_GRAY) {
                        if (it == MENUOPTION_DELETE) {
                            if (currentRuleIndex == index)
                                currentRuleIndex = null

                            tileType.removeRule(rule)
                        }
                    }
                }

                gui.plusButton(imageSize) {
                    tileType.addRule()
                }

                gui.ninesliceButton(imageSize) {
                    currentTileType?.let {
                        ruleGeneratorPopup.reset(tempTileSet, it)
                        gui.popup(ruleGeneratorPopup)
                    }
                }
            }
        }

        fun drawRuleEditor(x: Float, y: Float, width: Float, height: Float) {
            if (currentTileType == null)
                return

            val tileType = tempTileSet[requireNotNull(currentTileType)] ?: return
            val rule = tileType.rules[currentRuleIndex ?: return]
            val material = Game.assets.getMaterial(rule.material) ?: return

            val cellSize = min(width, height) * 0.8f / 3.0f

            val centerCellX = x + (width - cellSize * 3.0f) * 0.5f + cellSize
            val centerCellY = y + (height - cellSize * 3.0f) * 0.5f + cellSize

            fun drawDependency(x: Float, y: Float, dependency: TileSet.TileType.Dependency?, index: Int): TileSet.TileType.Dependency? {
                gui.setLastElement(gui.absolute(x, y))

                var newType: TileSet.TileType.Dependency.Type? = dependency?.type
                var typeToAdd: String? = null

                gui.ruleDependencyTypeEditor({
                    gui.bordered(Game.editorStyle.ruleEditorCellBorderColor, Game.editorStyle.ruleEditorCellBorderSize) {
                        gui.droppable<TileTypeData>({
                            newType = TileSet.TileType.Dependency.Type.TILE
                            typeToAdd = it.name
                        }, 2.5f) {
                            gui.group {
                                gui.colorRectangle(Game.editorStyle.ruleEditorCellBackgroundColor, cellSize)

                                gui.transient(ignoreGroup = true) {
                                    val cellContentX = x + Game.editorStyle.ruleEditorCellBorderSize
                                    val cellContentY = y + Game.editorStyle.ruleEditorCellBorderSize

                                    gui.setLastElement(gui.absolute(cellContentX, cellContentY))

                                    when (dependency?.type) {
                                        TileSet.TileType.Dependency.Type.TILE, TileSet.TileType.Dependency.Type.TILE_EXCLUSIVE -> {
                                            val previewContentSize = cellSize - Game.editorStyle.ruleEditorCellBorderSize * 2.0f

                                            val tileTypes = if (dependency is TileSet.TileType.TileTypeDependency)
                                                dependency.tileTypes
                                            else
                                                (dependency as TileSet.TileType.ExclusiveTileTypeDependency).tileTypes

                                            if (tileTypes.isNotEmpty()) {
                                                if (tileTypes.size == 1) {
                                                    val previewMaterial = Game.assets.getMaterial(tempTileSet[tileTypes.first()]?.getMaterial() ?: "<missing>") ?: Game.graphics2d.missingMaterial
                                                    gui.materialPreview(previewMaterial, previewContentSize)
                                                } else {
                                                    val previewImagesPerRow = 2

                                                    val previewImageSize = if (tileTypes.size > 4)
                                                        previewContentSize / previewImagesPerRow - gui.skin.scrollbarSize
                                                    else
                                                        previewContentSize / previewImagesPerRow

                                                    gui.scrollArea(maxHeight = previewContentSize, scroll = ruleEditorScrollAmounts[index]) {
                                                        val previewImages = tileTypes.mapTo(arrayListOf()) {
                                                            {
                                                                val previewMaterial = Game.assets.getMaterial(tempTileSet[it]?.getMaterial() ?: "<missing>") ?: Game.graphics2d.missingMaterial
                                                                gui.elementMenu({
                                                                    gui.materialPreview(previewMaterial, previewImageSize)
                                                                }, previewImageSize * 0.25f, arrayOf(MENUOPTION_DELETE), backgroundColor = Color.DARK_GRAY) {
                                                                    if (it == MENUOPTION_DELETE) {
                                                                        tileTypes.remove(it)
                                                                        if (tileTypes.isEmpty())
                                                                            newType = null
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        gui.multilineListWithSameElementWidths(previewImageSize * 2, previewImageSize) {
                                                            previewImages.removeFirstOrNull()
                                                        }
                                                    }
                                                }

                                                gui.setLastElement(gui.absolute(cellContentX, cellContentY))
                                                gui.colorRectangle(Color(1.0f, 1.0f, 1.0f, 0.33f), cellSize, cellSize)

                                                val label = {
                                                    if (dependency is TileSet.TileType.TileTypeDependency)
                                                        gui.label("Any of", maxWidth = cellSize, backgroundColor = Color.DARK_GRAY)
                                                    else
                                                        gui.label("Any except", maxWidth = cellSize, backgroundColor = Color.DARK_GRAY, overrideFontColor = Color.SCARLET)
                                                }

                                                val labelSize = gui.getElementSize(label)

                                                gui.setLastElement(gui.absolute(x + (cellSize - labelSize.width) * 0.5f, y + (cellSize - labelSize.height) * 0.5f))
                                                label()
                                            } else {
                                                val label = {
                                                    gui.label("Drag tiles here", maxWidth = cellSize)
                                                }

                                                val labelSize = gui.getElementSize(label)

                                                gui.setLastElement(gui.absolute(x + (cellSize - labelSize.width) * 0.5f, y + (cellSize - labelSize.height) * 0.5f))
                                                label()
                                            }
                                        }
                                        else -> {
                                            val text = when (dependency?.type) {
                                                TileSet.TileType.Dependency.Type.EMPTY -> "Empty"
                                                TileSet.TileType.Dependency.Type.SOLID -> "Solid"
                                                else -> "Anything"
                                            }

                                            val label = {
                                                gui.label(text, maxWidth = cellSize)
                                            }

                                            val labelSize = gui.getElementSize(label)

                                            gui.setLastElement(gui.absolute(x + (cellSize - labelSize.width) * 0.5f, y + (cellSize - labelSize.height) * 0.5f))
                                            label()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }, dependency?.type, openRuleEditorIndex == index, {
                    openRuleEditorIndex = if (it) index else null
                }) {
                    newType = it
                }

                if (dependency?.type == TileSet.TileType.Dependency.Type.TILE)
                    typeToAdd?.let {
                        (dependency as TileSet.TileType.TileTypeDependency).tileTypes += it
                    }

                return if (newType != dependency?.type) {
                    ruleEditorScrollAmounts[index].setZero()

                    when (newType) {
                        TileSet.TileType.Dependency.Type.EMPTY -> TileSet.TileType.EmptyDependency
                        TileSet.TileType.Dependency.Type.SOLID -> TileSet.TileType.SolidDependency
                        TileSet.TileType.Dependency.Type.TILE -> {
                            val newDependency = TileSet.TileType.TileTypeDependency()
                            if (dependency is TileSet.TileType.ExclusiveTileTypeDependency)
                                newDependency.tileTypes.addAll(dependency.tileTypes)
                            typeToAdd?.let {
                                newDependency.tileTypes += it
                            }
                            newDependency
                        }
                        TileSet.TileType.Dependency.Type.TILE_EXCLUSIVE -> {
                            val newDependency = TileSet.TileType.ExclusiveTileTypeDependency()
                            if (dependency is TileSet.TileType.TileTypeDependency)
                                newDependency.tileTypes.addAll(dependency.tileTypes)
                            newDependency
                        }
                        else -> null
                    }
                } else
                    dependency
            }

            gui.transient {
                gui.setLastElement(gui.absolute(centerCellX, centerCellY))
                gui.selectable({
                    gui.materialPreview(material, cellSize, cellSize)
                }, currentMaterial == rule.material) {
                    currentMaterial = rule.material
                }

                rule.dependencyTopLeft = drawDependency(centerCellX - cellSize, centerCellY - cellSize, rule.dependencyTopLeft, 0)
                rule.dependencyTopCenter = drawDependency(centerCellX, centerCellY - cellSize, rule.dependencyTopCenter, 1)
                rule.dependencyTopRight = drawDependency(centerCellX + cellSize, centerCellY - cellSize, rule.dependencyTopRight, 2)
                rule.dependencyCenterLeft = drawDependency(centerCellX - cellSize, centerCellY, rule.dependencyCenterLeft, 3)
                rule.dependencyCenterRight = drawDependency(centerCellX + cellSize, centerCellY, rule.dependencyCenterRight, 4)
                rule.dependencyBottomLeft = drawDependency(centerCellX - cellSize, centerCellY + cellSize, rule.dependencyBottomLeft, 5)
                rule.dependencyBottomCenter = drawDependency(centerCellX, centerCellY + cellSize, rule.dependencyBottomCenter, 6)
                rule.dependencyBottomRight = drawDependency(centerCellX + cellSize, centerCellY + cellSize, rule.dependencyBottomRight, 7)
            }
        }

        fun drawAssetSelector(width: Float, height: Float) {
            gui.transient {
                gui.setLastElement(gui.absolute(0.0f, Kore.graphics.height - height))
                gui.assetSelector(assetSelectorData, width, height)
            }
        }

        gui.setLastElement(gui.absolute(0.0f, 0.0f))

        return gui.group {
            drawBackground()
            drawTitle()

            val leftColumn = gui.sameLine {
                drawTileList()
                drawTileTypeEditor()
            }

            val rightColumnWidth = currentMaterial?.let {
                drawMaterialEditor(it)
            } ?: 0.0f

            val assetSelectorWidth = Kore.graphics.width.toFloat()
            val assetSelectorHeight = Kore.graphics.height * Game.editorStyle.assetSelectorHeight

            val ruleEditorWidth = Kore.graphics.width - leftColumn.width - rightColumnWidth
            val ruleEditorHeight = Kore.graphics.height - gui.skin.elementSize - assetSelectorHeight

            drawRuleEditor(leftColumn.width, gui.skin.elementSize, ruleEditorWidth, ruleEditorHeight)
            drawAssetSelector(assetSelectorWidth, assetSelectorHeight)
        }
    }
}