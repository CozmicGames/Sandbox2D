package game.level.ui

import com.cozmicgames.files.writeString
import engine.Game
import engine.graphics.font.HAlign
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.GUIPopup
import engine.graphics.ui.widgets.*
import engine.graphics.Material
import engine.assets.getAssetFileHandle
import engine.assets.managers.getMaterial

class MaterialEditorPopup : GUIPopup() {
    private lateinit var materialName: String
    private val material = Material()
    private val materialEditorData = MaterialEditorData()

    fun reset(materialName: String) {
        this.materialName = materialName
    }

    override fun draw(gui: GUI, width: Float, height: Float): GUIElement {
        return gui.dropShadow(Game.editorStyle.materialEditorPopupDropShadowColor) {
            gui.bordered(Game.editorStyle.materialEditorPopupBorderColor, Game.editorStyle.materialEditorPopupBorderSize) {
                gui.group(Game.editorStyle.materialEditorPopupContentBackgroundColor) {
                    val cancelButton = {
                        gui.textButton("Cancel") {
                            closePopup()
                        }
                    }

                    val cancelButtonSize = if (width > 0.0f) gui.getElementSize(cancelButton).width else 0.0f

                    val createButton = {
                        gui.textButton("Save") {
                            Game.assets.getMaterial(materialName)?.set(material)
                            Game.assets.getAssetFileHandle(materialName)?.writeString(material.write(), false)
                            closePopup()
                        }
                    }

                    val createButtonSize = if (width > 0.0f) gui.getElementSize(createButton).width else 0.0f

                    gui.label("Edit material", Game.editorStyle.materialEditorPopupTitleBackgroundColor, minWidth = if (width > 0.0f) width else null, align = HAlign.CENTER)

                    gui.materialEditor(material, materialEditorData)

                    gui.group(Game.editorStyle.materialEditorPopupTitleBackgroundColor) {
                        gui.sameLine {
                            cancelButton()
                            gui.spacing(width - cancelButtonSize - createButtonSize)
                            createButton()
                        }
                    }
                }
            }
        }
    }
}