package game.assets.types

import com.cozmicgames.*
import com.cozmicgames.files.FileHandle
import com.cozmicgames.utils.Color
import engine.Game
import engine.graphics.shaders.Shader
import engine.graphics.ui.DragDropData
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.widgets.image
import engine.graphics.ui.widgets.label
import game.assets.AssetType
import engine.assets.managers.getTexture
import engine.assets.managers.shaders
import engine.assets.remove
import game.extensions.MENUOPTION_DELETE
import game.extensions.elementMenu
import game.extensions.importButton
import game.level.ui.editorStyle

class ShaderAssetType : AssetType<Shader> {
    inner class ShaderImportPopup : SimpleImportPopup(this, "Import shader") {
        override fun onImport(file: FileHandle, name: String) {
            Game.assets.shaders?.add(file, name)
        }
    }

    class ShaderAsset(val name: String)

    override val assetType = Shader::class

    override val name = "Shaders"

    override val iconName = "internal/images/assettype_shader.png"

    private val importPopup = ShaderImportPopup()

    override fun preview(gui: GUI, size: Float, name: String, showMenu: Boolean) {
        if (showMenu)
            gui.elementMenu({
                gui.image(Game.assets.getTexture("internal/images/assettype_shader.png"), size)
            }, gui.skin.elementSize * 0.66f, arrayOf(MENUOPTION_DELETE), backgroundColor = Color.DARK_GRAY) {
                if (it == MENUOPTION_DELETE)
                    Game.assets.remove(name)
            }
        else
            gui.image(Game.assets.getTexture("internal/images/assettype_shader.png"), size)
    }

    override fun createDragDropData(name: String) = { DragDropData(ShaderAsset(name)) { label(name) } }

    override fun appendToAssetList(gui: GUI, list: MutableList<() -> GUIElement>) {
        list += {
            gui.importButton(Game.editorStyle.assetElementWidth) {
                Kore.dialogs.open("Open file", filters = arrayOf("shader"))?.let {
                    importPopup.reset(it)
                    gui.popup(importPopup)
                }
            }
        }
    }
}