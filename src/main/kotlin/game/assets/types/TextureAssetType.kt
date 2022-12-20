package game.assets.types

import com.cozmicgames.Kore
import com.cozmicgames.dialogs
import com.cozmicgames.files
import com.cozmicgames.files.nameWithExtension
import com.cozmicgames.graphics
import com.cozmicgames.graphics.Image
import com.cozmicgames.graphics.gpu.Texture
import com.cozmicgames.graphics.gpu.Texture2D
import com.cozmicgames.graphics.split
import com.cozmicgames.graphics.toTexture2D
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.extensions.extension
import com.cozmicgames.utils.extensions.nameWithoutExtension
import com.cozmicgames.utils.string
import engine.Game
import engine.assets.MetaFile
import engine.graphics.TextureRegion
import engine.graphics.asRegion
import engine.graphics.ui.*
import engine.graphics.ui.widgets.*
import game.assets.AssetType
import engine.assets.managers.TextureManager
import engine.assets.managers.getTexture
import engine.assets.managers.textures
import engine.assets.remove
import game.extensions.*
import game.level.ui.editorStyle
import kotlin.math.max
import kotlin.math.min

class TextureAssetType : AssetType<TextureRegion>, Disposable {
    class TextureMetaFile : MetaFile() {
        var filter by string { Texture.Filter.NEAREST.name }
    }

    private enum class ImportMode {
        SINGLE,
        SPLIT
    }

    inner class ImageImportPopup : ImportPopup(this, "Import texture"), Disposable {
        lateinit var file: String

        private lateinit var image: Image
        private var previewTexture: Texture2D? = null

        private val filterComboboxData = ComboboxData(*Texture.Filter.values())
        private val importModeComboboxData = ComboboxData(*ImportMode.values())
        private var excludeEmptyImages = true

        private val tileWidthTextData = TextData {
            val width = text.toIntOrNull()
            if (width != null) {
                tileWidth = width
                overrideFontColor = null
            } else
                overrideFontColor = Color.SCARLET
        }

        private val tileHeightTextData = TextData {
            val height = text.toIntOrNull()
            if (height != null) {
                tileHeight = height
                overrideFontColor = null
            } else
                overrideFontColor = Color.SCARLET
        }

        private val nameTextData = TextData {}

        private var tileWidth: Int
            get() = tileWidthTextData.text.toIntOrNull() ?: 1
            set(value) = tileWidthTextData.setText(max(1, value).toString())

        private var tileHeight: Int
            get() = tileHeightTextData.text.toIntOrNull() ?: 1
            set(value) = tileHeightTextData.setText(max(1, value).toString())

        private var leftSlice = 1.0f / 3.0f
        private var rightSlice = 1.0f / 3.0f
        private var topSlice = 1.0f / 3.0f
        private var bottomSlice = 1.0f / 3.0f

        override fun reset(file: String) {
            this.file = file
            nameTextData.setText(file.nameWithoutExtension)

            previewTexture?.dispose()
            image = requireNotNull(Kore.graphics.readImage(Kore.files.absolute(file)))
            previewTexture = image.toTexture2D(Game.graphics2d.pointClampSampler)

            filterComboboxData.selectedIndex = 0
            importModeComboboxData.selectedIndex = 0
            excludeEmptyImages = true
            tileWidth = 1
            tileHeight = 1
            leftSlice = 1.0f / 3.0f
            rightSlice = 1.0f / 3.0f
            topSlice = 1.0f / 3.0f
            bottomSlice = 1.0f / 3.0f
        }

        override fun drawContent(gui: GUI, width: Float, height: Float) {
            previewTexture?.let {
                it.setSampler(
                    when (filterComboboxData.selectedItem) {
                        Texture.Filter.LINEAR -> Game.graphics2d.linearClampSampler
                        else -> Game.graphics2d.pointClampSampler
                    }
                )

                val previewImageWidth = Game.editorStyle.imageImportPreviewSize * min(Kore.graphics.width, Kore.graphics.height)
                val previewImageHeight = previewImageWidth * image.height.toFloat() / image.width.toFloat()

                val (linesX, linesY) = gui.getLastElement()

                val previewImageOffset = (width - previewImageWidth) * 0.5f

                gui.offset(previewImageOffset, 0.0f, resetX = true) {
                    gui.image(it.asRegion(), previewImageWidth, previewImageHeight, borderThickness = 0.0f)
                }

                if (importModeComboboxData.selectedItem == ImportMode.SPLIT) {
                    val linesHorizontal = image.width / tileWidth + 1
                    val linesVertical = image.height / tileHeight + 1

                    val lineSpacingHorizontal = previewImageWidth / (linesHorizontal - 1)
                    val lineSpacingVertical = previewImageHeight / (linesVertical - 1)

                    repeat(linesHorizontal) {
                        val x = previewImageOffset + linesX + it * lineSpacingHorizontal
                        gui.currentCommandList.drawLine(x, linesY, x, linesY + previewImageHeight, 3.0f, gui.skin.fontColor)
                    }

                    repeat(linesVertical) {
                        val y = linesY + it * lineSpacingVertical
                        gui.currentCommandList.drawLine(previewImageOffset + linesX, y, previewImageOffset + linesX + previewImageWidth, y, 3.0f, gui.skin.fontColor)
                    }
                }
            }

            if (importModeComboboxData.selectedItem == ImportMode.SPLIT) {
                val columns = image.width / max(tileWidth, 1)
                val rows = image.height / max(tileHeight, 1)

                gui.label("$columns x $rows tiles", null)
            }

            gui.sameLine {
                val labelsWidth = gui.group {
                    gui.label("Import mode", null)
                    gui.label("Filter", null)
                    if (importModeComboboxData.selectedItem == ImportMode.SPLIT) {
                        gui.label("Exclude empty images", null)
                        gui.label("Tile width", null)
                        gui.label("Tile height", null)
                    }
                    gui.label("Original filename", null)
                    gui.label("Import filename", null)
                }.width

                gui.group {
                    gui.combobox(importModeComboboxData)
                    gui.combobox(filterComboboxData)
                    if (importModeComboboxData.selectedItem == ImportMode.SPLIT) {
                        gui.checkBox(excludeEmptyImages) { excludeEmptyImages = it }
                        gui.sameLine {
                            gui.textField(tileWidthTextData)
                            gui.group {
                                gui.upButton(gui.skin.elementSize * 0.5f) {
                                    tileWidth++
                                }
                                gui.downButton(gui.skin.elementSize * 0.5f) {
                                    tileWidth--
                                }
                            }
                        }
                        gui.sameLine {
                            gui.textField(tileHeightTextData)
                            gui.group {
                                gui.upButton(gui.skin.elementSize * 0.5f) {
                                    tileHeight++
                                }
                                gui.downButton(gui.skin.elementSize * 0.5f) {
                                    tileHeight--
                                }
                            }
                        }
                    }
                    gui.tooltip(gui.label(file, null, maxWidth = width - labelsWidth), file)
                    gui.textField(nameTextData, labelsWidth)
                }
            }
        }

        override fun onImport() {
            val selectedFilter = filterComboboxData.selectedItem ?: Texture.Filter.NEAREST
            val selectedMode = importModeComboboxData.selectedItem

            when (selectedMode) {
                ImportMode.SPLIT -> {
                    val columns = image.width / max(tileWidth, 1)
                    val rows = image.height / max(tileHeight, 1)
                    val images = image.split(columns, rows)

                    repeat(images.width) { x ->
                        repeat(images.height) { y ->
                            images[x, y]?.let {
                                if (!(excludeEmptyImages && it.pixels.data.all { it.data.all { it == 0.0f } })) {
                                    val imageFileName = "${nameTextData.text}_${x}_${y}.${file.extension}"
                                    val assetFile = Game.assets.toAssetFileHandle(imageFileName)

                                    if (assetFile.exists)
                                        assetFile.delete()

                                    Kore.graphics.writeImage(assetFile, it)

                                    val metaFile = TextureMetaFile()
                                    metaFile.name = imageFileName
                                    metaFile.filter = selectedFilter.name
                                    metaFile.write(assetFile.sibling("${assetFile.nameWithExtension}.meta"))

                                    Game.assets.textures?.add(imageFileName, it, TextureManager.TextureParams(selectedFilter))
                                }
                            }
                        }
                    }
                }
                else -> {
                    val imageFileName = "${nameTextData.text}.${file.extension}"
                    val assetFile = Game.assets.toAssetFileHandle(imageFileName)

                    if (file != nameTextData.text) {
                        if (assetFile.exists)
                            assetFile.delete()

                        Kore.files.absolute(file).copyTo(assetFile)
                    }

                    val metaFile = TextureMetaFile()
                    metaFile.name = imageFileName
                    metaFile.filter = selectedFilter.name
                    metaFile.write(assetFile.sibling("${assetFile.nameWithExtension}.meta"))

                    Game.assets.textures?.add(assetFile, imageFileName, params = TextureManager.TextureParams(selectedFilter))
                }
            }
        }

        override fun dispose() {
            previewTexture?.dispose()
        }
    }

    class TextureAsset(val name: String)

    override val assetType = TextureRegion::class

    override val name = "Textures"

    override val iconName = "internal/images/assettype_texture.png"

    private val imageImportPopup = ImageImportPopup()

    override fun preview(gui: GUI, size: Float, name: String, showMenu: Boolean) {
        if (showMenu)
            gui.elementMenu({
                gui.image(Game.assets.getTexture(name), size)
            }, gui.skin.elementSize * 0.66f, arrayOf(MENUOPTION_DELETE), backgroundColor = Color.DARK_GRAY) {
                if (it == MENUOPTION_DELETE)
                    Game.assets.remove(name)
            }
        else
            gui.image(Game.assets.getTexture(name), size)
    }

    override fun createDragDropData(name: String) = { DragDropData(TextureAsset(name)) { image(Game.assets.getTexture(name), Game.editorStyle.assetElementWidth) } }

    override fun appendToAssetList(gui: GUI, list: MutableList<() -> GUIElement>) {
        list += {
            gui.importButton(Game.editorStyle.assetElementWidth) {
                Kore.dialogs.open("Open file", filters = Kore.graphics.supportedImageFormats.toList().toTypedArray())?.let {
                    imageImportPopup.reset(it)
                    gui.popup(imageImportPopup)
                }
            }
        }
    }

    override fun dispose() {
        imageImportPopup.dispose()
    }
}