package game.assets

import engine.graphics.ui.DragDropData
import engine.graphics.ui.GUI
import engine.graphics.ui.GUIElement
import kotlin.reflect.KClass

interface AssetType<T : Any> {
    val assetType: KClass<T>
    val name: String
    val iconName: String

    fun preview(gui: GUI, size: Float, name: String, showMenu: Boolean)

    fun createDragDropData(name: String): () -> DragDropData<*>

    fun appendToAssetList(gui: GUI, list: MutableList<() -> GUIElement>)
}