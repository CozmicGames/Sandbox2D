package game.assets

import com.cozmicgames.utils.maths.Vector2
import engine.graphics.ui.TextData
import kotlin.reflect.KClass

class AssetSelectorData {
    var showInternalAssetElements = false
    var currentAssetType: KClass<*>? = null
    var showEditIcons = false
    var filter: List<KClass<*>> = emptyList()
    val elementsScroll = Vector2()
    val assetTitleScroll = Vector2()
    val filterTextData = TextData { }
    var currentFolder: String? = null
}