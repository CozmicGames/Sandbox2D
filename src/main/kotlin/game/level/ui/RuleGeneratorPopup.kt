package game.level.ui

import com.cozmicgames.Kore
import com.cozmicgames.files.nameWithExtension
import com.cozmicgames.graphics
import com.cozmicgames.graphics.Image
import com.cozmicgames.graphics.gpu.Texture
import com.cozmicgames.utils.extensions.extension
import com.cozmicgames.utils.extensions.pathWithoutExtension
import engine.Game
import engine.graphics.TextureRegion
import engine.graphics.font.HAlign
import engine.graphics.ui.*
import engine.graphics.ui.widgets.*
import engine.assets.getAssetFileHandle
import engine.assets.managers.TextureManager
import engine.assets.managers.getMaterial
import engine.assets.managers.getTexture
import engine.assets.managers.getTextureFilter
import game.assets.types.TextureAssetType
import game.extensions.materialPreview
import game.level.TileSet
import kotlin.math.min

class RuleGeneratorPopup : GUIPopup() {
    private lateinit var tileSet: TileSet
    private lateinit var tileTypeName: String
    private var leftSlice = 1.0f / 3.0f
    private var rightSlice = 1.0f / 3.0f
    private var topSlice = 1.0f / 3.0f
    private var bottomSlice = 1.0f / 3.0f
    private val nameTextData = TextData {}
    private val filterComboboxData = ComboboxData(*Texture.Filter.values())
    private var imageFileName: String? = null

    fun reset(tileSet: TileSet, tileTypeName: String) {
        this.tileSet = tileSet
        this.tileTypeName = tileTypeName
        leftSlice = 1.0f / 3.0f
        rightSlice = 1.0f / 3.0f
        topSlice = 1.0f / 3.0f
        bottomSlice = 1.0f / 3.0f

        tileSet[tileTypeName]?.let { tileType ->
            Game.assets.getMaterial(tileType.defaultMaterial)?.let { material ->
                imageFileName = material.colorTexturePath
                nameTextData.setText(material.colorTexturePath.pathWithoutExtension)
                filterComboboxData.selectedIndex = Game.assets.getTextureFilter(material.colorTexturePath)?.ordinal ?: 0
            }
        }
    }

    override fun draw(gui: GUI, width: Float, height: Float): GUIElement {
        return gui.dropShadow(Game.editorStyle.ruleGeneratorPopupDropShadowColor) {
            gui.bordered(Game.editorStyle.ruleGeneratorPopupBorderColor, Game.editorStyle.ruleGeneratorPopupBorderSize) {
                gui.group(Game.editorStyle.ruleGeneratorPopupContentBackgroundColor) {
                    val cancelButton = {
                        gui.textButton("Cancel") {
                            closePopup()
                        }
                    }

                    val cancelButtonSize = if (width > 0.0f) gui.getElementSize(cancelButton).width else 0.0f

                    val generateButton = {
                        gui.textButton("Generate") {
                            val selectedFilter = filterComboboxData.selectedItem ?: Texture.Filter.NEAREST

                            imageFileName?.let { imageFileName ->
                                Game.assets.getAssetFileHandle(imageFileName)?.let {
                                    Kore.graphics.readImage(it)?.let { image ->
                                        val leftWidth = (image.width * leftSlice).toInt()
                                        val rightWidth = (image.width * rightSlice).toInt()
                                        val centerWidth = image.width - leftWidth - rightWidth

                                        val topHeight = (image.height * topSlice).toInt()
                                        val bottomHeight = (image.height * bottomSlice).toInt()
                                        val centerHeight = image.height - topHeight - bottomHeight

                                        val sliceTopLeft = image.getSubImage(0, 0, leftWidth, topHeight)
                                        val sliceTopCenter = image.getSubImage(leftWidth, 0, centerWidth, topHeight)
                                        val sliceTopRight = image.getSubImage(leftWidth + centerWidth, 0, rightWidth, topHeight)

                                        val sliceCenterLeft = image.getSubImage(0, topHeight, leftWidth, centerHeight)
                                        val sliceCenterCenter = image.getSubImage(leftWidth, topHeight, centerWidth, centerHeight)
                                        val sliceCenterRight = image.getSubImage(leftWidth + centerWidth, topHeight, rightWidth, centerHeight)

                                        val sliceBottomLeft = image.getSubImage(0, topHeight + centerHeight, leftWidth, bottomHeight)
                                        val sliceBottomCenter = image.getSubImage(leftWidth, topHeight + centerHeight, centerWidth, bottomHeight)
                                        val sliceBottomRight = image.getSubImage(leftWidth + centerWidth, topHeight + centerHeight, rightWidth, bottomHeight)

                                        var variant = 0

                                        fun addVariant(topLeft: Image, topRight: Image, bottomLeft: Image, bottomRight: Image, sameTileTop: Boolean, sameTileRight: Boolean, sameTileBottom: Boolean, sameTileLeft: Boolean) {
                                            val variantImage = Image(image.width * 2 / 3, image.height * 2 / 3)
                                            variantImage.setImage(topLeft, 0, 0)
                                            variantImage.setImage(topRight, variantImage.width / 2, 0)
                                            variantImage.setImage(bottomLeft, 0, variantImage.height / 2)
                                            variantImage.setImage(bottomRight, variantImage.width / 2, variantImage.height / 2)

                                            val variantImageFileName = "${nameTextData.text}.${variant++}.${imageFileName.extension}"
                                            val assetFile = Game.assets.toAssetFileHandle(variantImageFileName)

                                            tileSet[tileTypeName]?.let {
                                                val rule = it.addRule()
                                                Game.assets.getMaterial(rule.material)?.colorTexturePath = variantImageFileName

                                                rule.dependencyTopCenter = if (sameTileTop) TileSet.TileType.TileTypeDependency(tileTypeName) else TileSet.TileType.ExclusiveTileTypeDependency(tileTypeName)
                                                rule.dependencyCenterRight = if (sameTileRight) TileSet.TileType.TileTypeDependency(tileTypeName) else TileSet.TileType.ExclusiveTileTypeDependency(tileTypeName)
                                                rule.dependencyBottomCenter = if (sameTileBottom) TileSet.TileType.TileTypeDependency(tileTypeName) else TileSet.TileType.ExclusiveTileTypeDependency(tileTypeName)
                                                rule.dependencyCenterLeft = if (sameTileLeft) TileSet.TileType.TileTypeDependency(tileTypeName) else TileSet.TileType.ExclusiveTileTypeDependency(tileTypeName)
                                            }

                                            if (assetFile.exists)
                                                assetFile.delete()

                                            Kore.graphics.writeImage(assetFile, variantImage)

                                            val metaFile = TextureAssetType.TextureMetaFile()
                                            metaFile.name = variantImageFileName
                                            metaFile.filter = selectedFilter.name
                                            metaFile.write(assetFile.sibling("${assetFile.nameWithExtension}.meta"))

                                            (Game.assets.getAssetTypeManager<TextureRegion>() as? TextureManager)?.add(variantImageFileName, variantImage, TextureManager.TextureParams(selectedFilter), assetFile)
                                        }

                                        addVariant(sliceTopLeft, sliceTopRight, sliceBottomLeft, sliceBottomRight, false, false, false, false)
                                        addVariant(sliceCenterCenter, sliceCenterCenter, sliceCenterCenter, sliceCenterCenter, true, true, true, true)
                                        addVariant(sliceTopLeft, sliceTopCenter, sliceBottomLeft, sliceBottomCenter, false, true, false, false)
                                        addVariant(sliceTopCenter, sliceTopCenter, sliceBottomCenter, sliceBottomCenter, false, true, false, true)
                                        addVariant(sliceTopCenter, sliceTopRight, sliceBottomCenter, sliceBottomRight, false, false, false, true)
                                        addVariant(sliceTopLeft, sliceTopRight, sliceCenterLeft, sliceCenterRight, false, false, true, false)
                                        addVariant(sliceCenterLeft, sliceCenterRight, sliceCenterLeft, sliceCenterRight, true, false, true, false)
                                        addVariant(sliceCenterLeft, sliceCenterRight, sliceBottomLeft, sliceBottomRight, true, false, false, false)
                                        addVariant(sliceTopLeft, sliceTopCenter, sliceCenterLeft, sliceCenterCenter, false, true, true, false)
                                        addVariant(sliceTopCenter, sliceTopCenter, sliceCenterCenter, sliceCenterCenter, false, true, true, true)
                                        addVariant(sliceTopCenter, sliceTopRight, sliceCenterCenter, sliceCenterRight, false, false, true, true)
                                        addVariant(sliceCenterCenter, sliceCenterRight, sliceCenterCenter, sliceCenterRight, true, false, true, true)
                                        addVariant(sliceCenterCenter, sliceCenterRight, sliceBottomCenter, sliceBottomRight, true, false, false, true)
                                        addVariant(sliceCenterCenter, sliceCenterCenter, sliceBottomCenter, sliceBottomCenter, true, true, false, true)
                                        addVariant(sliceCenterLeft, sliceCenterCenter, sliceBottomLeft, sliceBottomCenter, true, true, false, false)
                                        addVariant(sliceCenterLeft, sliceCenterCenter, sliceCenterLeft, sliceCenterCenter, true, true, true, false)
                                    }
                                }
                            }

                            closePopup()
                        }
                    }

                    val generateButtonSize = if (width > 0.0f) gui.getElementSize(generateButton).width else 0.0f

                    gui.label("Generate rules", Game.editorStyle.ruleGeneratorPopupTitleBackgroundColor, minWidth = if (width > 0.0f) width else null, align = HAlign.CENTER)

                    gui.blankLine()

                    val previewSize = min(width, height)
                    val previewOffset = (width - previewSize) * 0.5f

                    tileSet[tileTypeName]?.let {
                        Game.assets.getMaterial(it.defaultMaterial)?.let {
                            gui.offset(previewOffset, 0.0f, resetX = true) {
                                val (linesX, linesY) = gui.getLastElement()

                                gui.materialPreview(it, previewSize)

                                val region = Game.assets.getTexture(it.colorTexturePath)

                                val pixelSizeX = previewSize / region.width
                                val pixelSizeY = previewSize / region.height

                                val leftLineX = linesX + region.width * leftSlice * pixelSizeX
                                val rightLineX = linesX + region.width * (1.0f - rightSlice) * pixelSizeX
                                val topLineY = linesY + region.height * topSlice * pixelSizeY
                                val bottomLineY = linesY + region.height * (1.0f - bottomSlice) * pixelSizeY

                                gui.currentCommandList.drawLine(leftLineX, linesY, leftLineX, linesY + previewSize, 3.0f, gui.skin.fontColor)
                                gui.currentCommandList.drawLine(rightLineX, linesY, rightLineX, linesY + previewSize, 3.0f, gui.skin.fontColor)
                                gui.currentCommandList.drawLine(linesX, topLineY, linesX + previewSize, topLineY, 3.0f, gui.skin.fontColor)
                                gui.currentCommandList.drawLine(linesX, bottomLineY, linesX + previewSize, bottomLineY, 3.0f, gui.skin.fontColor)
                            }
                        }
                    }

                    gui.blankLine()

                    gui.sameLine {
                        val labelsWidth = gui.group {
                            gui.label("Filter", null)
                            gui.label("Original path", null)
                            gui.label("Generated paths", null)
                        }.width

                        gui.group {
                            gui.combobox(filterComboboxData)
                            gui.tooltip(gui.label(imageFileName.orEmpty(), null, maxWidth = width - labelsWidth), imageFileName.orEmpty())
                            gui.textField(nameTextData, labelsWidth)
                        }
                    }

                    gui.blankLine()

                    gui.group(Game.editorStyle.ruleGeneratorPopupTitleBackgroundColor) {
                        gui.sameLine {
                            cancelButton()
                            gui.spacing(width - cancelButtonSize - generateButtonSize)
                            generateButton()
                        }
                    }
                }
            }
        }
    }
}