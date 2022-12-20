package engine.assets.managers

import com.cozmicgames.Kore
import com.cozmicgames.files.FileHandle
import com.cozmicgames.files.readToString
import com.cozmicgames.log
import com.cozmicgames.utils.Properties
import engine.assets.AssetManager
import game.level.TileSet

class TileSetManager : StandardAssetTypeManager<TileSet, Unit>(TileSet::class) {
    override val supportedFormats = setOf("tileset")

    override val defaultParams = Unit

    override fun add(file: FileHandle, name: String, params: Unit) {
        if (!file.exists) {
            Kore.log.error(this::class, "Tileset file not found: $file")
            return
        }

        val tileSet = TileSet(name)

        try {
            tileSet.read(Properties().also { it.read(file.readToString()) })
        } catch (e: Exception) {
            Kore.log.error(this::class, "Failed to load tileset file: $file")
            return
        }

        add(name, tileSet, file)
    }
}

val AssetManager.tileSets get() = getAssetTypeManager<TileSet>() as? TileSetManager

fun AssetManager.getTileSet(name: String) = getAsset(name, TileSet::class)
