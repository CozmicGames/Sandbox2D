package game.level.ui

import com.cozmicgames.utils.Color
import engine.graphics.ui.TextData
import engine.graphics.Material

class MaterialEditorData : TextData() {
    var material: Material? = null

    init {
        onEnter = {
            overrideFontColor = try {
                val color = Color.fromHexString(text)
                material?.color?.set(color)
                null
            } catch (_: Exception) {
                Color.SCARLET
            }
        }
    }
}