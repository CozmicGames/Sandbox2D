package game.assets.types

import engine.Game
import engine.graphics.font.HAlign
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.GUIPopup
import engine.graphics.ui.widgets.bordered
import engine.graphics.ui.widgets.dropShadow
import engine.graphics.ui.widgets.label
import engine.graphics.ui.widgets.textButton
import game.assets.AssetType
import game.level.ui.editorStyle

abstract class ImportPopup(val type: AssetType<*>, val titleString: String) : GUIPopup() {
    abstract fun reset(file: String)

    override fun draw(gui: GUI, width: Float, height: Float): GUIElement {
        return gui.dropShadow(Game.editorStyle.importPopupDropShadowColor) {
            gui.bordered(Game.editorStyle.importPopupBorderColor, Game.editorStyle.importPopupBorderSize) {
                gui.group(Game.editorStyle.importPopupContentBackgroundColor) {
                    val cancelButton = {
                        gui.textButton("Cancel") {
                            closePopup()
                        }
                    }

                    val cancelButtonSize = if (width > 0.0f) gui.getElementSize(cancelButton).width else 0.0f

                    val importButton = {
                        gui.textButton("Import") {
                            onImport()
                            closePopup()
                        }
                    }

                    val importButtonSize = if (width > 0.0f) gui.getElementSize(importButton).width else 0.0f

                    gui.label(titleString, Game.editorStyle.importPopupTitleBackgroundColor, minWidth = if (width > 0.0f) width else null, align = HAlign.CENTER)

                    drawContent(gui, width, height)

                    gui.group(Game.editorStyle.importPopupTitleBackgroundColor) {
                        gui.sameLine {
                            cancelButton()
                            gui.spacing(width - cancelButtonSize - importButtonSize)
                            importButton()
                        }
                    }
                }
            }
        }
    }

    protected abstract fun onImport()

    protected abstract fun drawContent(gui: GUI, width: Float, height: Float)
}

fun GUI.importPopup(file: String, popup: ImportPopup) {
    popup.reset(file)
    popup(popup)
}
