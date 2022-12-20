package game.assets.types

import com.cozmicgames.*
import com.cozmicgames.files.FileHandle
import com.cozmicgames.graphics.Font
import com.cozmicgames.utils.Color
import engine.Game
import engine.graphics.ui.DragDropData
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.widgets.image
import engine.graphics.ui.widgets.label
import game.assets.AssetType
import engine.assets.managers.fonts
import engine.assets.managers.getTexture
import engine.assets.remove
import game.extensions.MENUOPTION_DELETE
import game.extensions.elementMenu
import game.extensions.importButton
import game.level.ui.editorStyle

class FontAssetType : AssetType<Font> {
    inner class FontImportPopup : SimpleImportPopup(this, "Import font") {
        override fun onImport(file: FileHandle, name: String) {
            Game.assets.fonts?.add(file, name)
        }
    }

    class FontAsset(val name: String)

    override val assetType = Font::class

    override val name = "Fonts"

    override val iconName = "internal/images/assettype_font.png"

    private val importPopup = FontImportPopup()

    override fun preview(gui: GUI, size: Float, name: String, showMenu: Boolean) {
        if (showMenu)
            gui.elementMenu({
                gui.image(Game.assets.getTexture("internal/images/assettype_font.png"), size)
            }, gui.skin.elementSize * 0.66f, arrayOf(MENUOPTION_DELETE), backgroundColor = Color.DARK_GRAY) {
                if (it == MENUOPTION_DELETE)
                    Game.assets.remove(name)
            }
        else
            gui.image(Game.assets.getTexture("internal/images/assettype_font.png"), size)
    }

    override fun createDragDropData(name: String) = { DragDropData(FontAsset(name)) { label(name) } }

    override fun appendToAssetList(gui: GUI, list: MutableList<() -> GUIElement>) {
        list += {
            gui.importButton(Game.editorStyle.assetElementWidth) {
                Kore.dialogs.open("Open file", filters = Kore.graphics.supportedFontFormats.toList().toTypedArray())?.let {
                    importPopup.reset(it)
                    gui.popup(importPopup)
                }
            }
        }
    }
}