package game.level.ui

import engine.Game
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import engine.graphics.ui.widgets.*
import engine.graphics.Material
import engine.assets.managers.getMaterial
import game.assets.types.ShaderAssetType
import game.assets.types.TextureAssetType
import game.extensions.materialPreview


fun GUI.materialEditor(materialName: String, data: MaterialEditorData): GUIElement {
    val material = Game.assets.getMaterial(materialName) ?: return absolute(0.0f, 0.0f)
    return materialEditor(material, data)
}

fun GUI.materialEditor(material: Material, data: MaterialEditorData): GUIElement {
    data.material = material

    var width = 0.0f
    val editor = {
        group {
            label("Material", Game.editorStyle.panelTitleBackgroundColor, minWidth = width)
            materialPreview(material, width)
            separator(width)

            label("Texture", Game.editorStyle.panelTitleBackgroundColor, minWidth = width)
            droppable<TextureAssetType.TextureAsset>({ material.colorTexturePath = it.name }, 2.5f) {
                tooltip(label(material.colorTexturePath, null, maxWidth = width), material.colorTexturePath)
            }
            separator(width)

            label("Shader", Game.editorStyle.panelTitleBackgroundColor, minWidth = width)
            droppable<ShaderAssetType.ShaderAsset>({ material.shader = it.name }, 2.0f) {
                tooltip(label(material.shader, null, maxWidth = width), material.shader)
            }
            separator(width)

            label("Color", Game.editorStyle.panelTitleBackgroundColor, minWidth = width)

            colorEdit(material.color) {
                data.setText(material.color.toHexString())
            }

            sameLine {
                label("Hex: ", null)
                textField(data)
            }
        }
    }

    width = getElementSize(editor).width
    return editor()
}
