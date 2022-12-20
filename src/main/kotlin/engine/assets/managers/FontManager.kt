package engine.assets.managers

import com.cozmicgames.Kore
import com.cozmicgames.files.FileHandle
import com.cozmicgames.graphics
import com.cozmicgames.graphics.Font
import com.cozmicgames.log
import engine.assets.AssetManager

class FontManager : StandardAssetTypeManager<Font, Unit>(Font::class) {
    override val supportedFormats = Kore.graphics.supportedFontFormats.toSet()

    override val defaultParams = Unit

    override fun add(file: FileHandle, name: String, params: Unit) {
        if (!file.exists) {
            Kore.log.error(this::class, "Font file not found: $file")
            return
        }

        val font = Kore.graphics.readFont(file)

        if (font == null) {
            Kore.log.error(this::class, "Failed to load font file: $file")
            return
        }

        add(name, font)
    }
}

val AssetManager.fonts get() = getAssetTypeManager<Font>() as? FontManager

fun AssetManager.getFont(name: String) = getAsset(name, Font::class)
