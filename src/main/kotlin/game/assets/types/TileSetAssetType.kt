package game.assets.types

import com.cozmicgames.*
import com.cozmicgames.files.FileHandle
import com.cozmicgames.files.writeString
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.Properties
import engine.Game
import engine.graphics.ui.DragDropData
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.widgets.image
import engine.graphics.ui.widgets.label
import engine.assets.*
import engine.assets.managers.getTexture
import engine.assets.managers.tileSets
import game.assets.AssetType
import game.extensions.*
import game.level.TileSet
import game.level.ui.TileSetEditorPopup
import game.level.ui.editorStyle

class TileSetAssetType : AssetType<TileSet> {
    inner class TileSetImportPopup : SimpleImportPopup(this, "Import tileset") {
        override fun onImport(file: FileHandle, name: String) {
            Game.assets.tileSets?.add(file, name)
        }
    }

    class TileSetAsset(val name: String)

    override val assetType = TileSet::class

    override val name = "Tilesets"

    override val iconName = "internal/images/assettype_tileset.png"

    private val createFilePopup = CreateFilePopup("tileset")
    private val importPopup = TileSetImportPopup()
    private val editorPopup = TileSetEditorPopup()

    override fun preview(gui: GUI, size: Float, name: String, showMenu: Boolean) {
        if (showMenu) {
            val options = if (Game.assets.getAssetFileHandle(name)?.isWritable != false) arrayOf(MENUOPTION_EDIT, MENUOPTION_DELETE) else arrayOf(MENUOPTION_DELETE)

            gui.elementMenu({
                gui.image(Game.assets.getTexture("internal/images/assettype_tileset.png"), size)
            }, gui.skin.elementSize * 0.66f, options, backgroundColor = Color.DARK_GRAY) {
                when (it) {
                    MENUOPTION_EDIT -> Kore.onNextFrame {
                        editorPopup.reset(name)
                        gui.popup(editorPopup)
                    }
                    MENUOPTION_DELETE -> Game.assets.remove(name)
                }
            }
        } else
            gui.image(Game.assets.getTexture("internal/images/assettype_tileset.png"), size)
    }

    override fun createDragDropData(name: String) = { DragDropData(TileSetAsset(name)) { label(name) } }

    override fun appendToAssetList(gui: GUI, list: MutableList<() -> GUIElement>) {
        list += {
            gui.plusButton(Game.editorStyle.assetElementWidth) {
                createFilePopup.reset {
                    val assetFile = Kore.files.local("assets/$it")
                    assetFile.writeString(Properties().also { TileSet(name).write(it) }.write(true), false)
                    Game.assets.tileSets?.add(it, TileSet(it), assetFile)
                }
                gui.popup(createFilePopup)
            }
        }

        list += {
            gui.importButton(Game.editorStyle.assetElementWidth) {
                Kore.dialogs.open("Open file", filters = arrayOf("tileset"))?.let {
                    importPopup.reset(it)
                    gui.popup(importPopup)
                }
            }
        }
    }
}