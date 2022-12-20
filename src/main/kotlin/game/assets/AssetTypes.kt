package game.assets

import com.cozmicgames.Kore
import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.injector
import engine.Game
import game.assets.types.*
import kotlin.reflect.KClass

class AssetTypes : Disposable {
    private val registeredAssetTypes = hashSetOf<AssetType<*>>()

    val types get() = registeredAssetTypes.toSet()

    init {
        registerAssetType(TextureAssetType())
        registerAssetType(FontAssetType())
        registerAssetType(SoundAssetType())
        registerAssetType(MaterialAssetType())
        registerAssetType(TileSetAssetType())
        registerAssetType(ShaderAssetType())
    }

    fun registerAssetType(assetType: AssetType<*>): AssetType<*> {
        registeredAssetTypes.add(assetType)
        return assetType
    }

    fun findAssetType(predicate: (AssetType<*>) -> Boolean) = registeredAssetTypes.find(predicate)

    override fun dispose() {
        registeredAssetTypes.forEach {
            if (it is Disposable)
                it.dispose()
        }
    }
}

fun AssetTypes.findAssetType(type: KClass<*>) = findAssetType { it.assetType == type }

val Game.assetTypes by Kore.context.injector(true) { AssetTypes() }
