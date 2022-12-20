package engine.assets.managers

import com.cozmicgames.Kore
import com.cozmicgames.files.FileHandle
import com.cozmicgames.files.nameWithExtension
import com.cozmicgames.graphics
import com.cozmicgames.graphics.Image
import com.cozmicgames.graphics.gpu.Texture
import com.cozmicgames.log
import com.cozmicgames.utils.extensions.enumValueOfOrDefault
import engine.Game
import engine.graphics.TextureAtlas
import engine.graphics.TextureRegion
import engine.graphics.asRegion
import engine.assets.AssetManager
import engine.assets.AssetTypeManager
import engine.assets.MetaFile

class TextureManager : AssetTypeManager<TextureRegion, TextureManager.TextureParams>(TextureRegion::class) {
    data class TextureParams(var filter: Texture.Filter = Texture.Filter.NEAREST)

    override val supportedFormats = Kore.graphics.supportedImageFormats.toSet()

    override val defaultParams = TextureParams()

    class Entry(val params: TextureParams, val file: FileHandle?)

    private val textures = hashMapOf<TextureParams, TextureAtlas>()
    private val entries = hashMapOf<String, Entry>()

    override val names get() = entries.keys

    override fun getParams(metaFile: MetaFile): TextureParams {
        val filter = metaFile.getString("filter")?.let {
            enumValueOfOrDefault(it) { Texture.Filter.NEAREST }
        } ?: Texture.Filter.NEAREST

        return TextureParams(filter)
    }

    override fun add(file: FileHandle, name: String, params: TextureParams) {
        val image = if (file.exists)
            Kore.graphics.readImage(file)
        else {
            Kore.log.error(this::class, "Texture file not found: $file")
            return
        }

        if (image == null) {
            Kore.log.error(this::class, "Failed to load texture file: $file")
            return
        }

        add(name, image, params, file)
    }

    fun add(name: String, image: Image, params: TextureParams, file: FileHandle? = null) {
        val atlas = getAtlas(params)
        atlas.add(name to image)
        entries[name] = Entry(params, file)
    }

    fun getAtlas(params: TextureParams): TextureAtlas {
        return textures.getOrPut(params) {
            when (params.filter) {
                Texture.Filter.NEAREST -> TextureAtlas(sampler = Game.graphics2d.pointClampSampler)
                Texture.Filter.LINEAR -> TextureAtlas(sampler = Game.graphics2d.linearClampSampler)
            }
        }
    }

    override fun contains(name: String) = name in entries

    override fun get(name: String): TextureRegion? {
        val entry = entries[name] ?: return null
        val atlas = getAtlas(entry.params)
        return atlas[name]
    }

    override fun remove(name: String): Boolean {
        val entry = entries.remove(name) ?: return false

        val atlas = getAtlas(entry.params)
        atlas.remove(name)

        val file = entry.file

        if (file != null) {
            if (file.exists && file.isWritable)
                file.delete()

            val metaFile = file.sibling("${file.nameWithExtension}.meta")
            if (metaFile.exists && metaFile.isWritable)
                metaFile.delete()
        }

        return true
    }

    override fun getFileHandle(name: String): FileHandle? {
        val entry = entries[name] ?: return null
        return entry.file
    }

    fun getFilter(name: String): Texture.Filter? {
        val entry = entries[name] ?: return null
        return entry.params.filter
    }

    override fun dispose() {
        textures.forEach { _, texture ->
            texture.dispose()
        }
    }
}

val AssetManager.textures get() = getAssetTypeManager<TextureRegion>() as? TextureManager

fun AssetManager.getTexture(name: String) = getAsset(name, TextureRegion::class) ?: Game.graphics2d.missingTexture.asRegion()

fun AssetManager.getTextureFilter(name: String) = textures?.getFilter(name)
