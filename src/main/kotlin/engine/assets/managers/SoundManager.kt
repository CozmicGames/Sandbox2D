package engine.assets.managers

import com.cozmicgames.Kore
import com.cozmicgames.audio
import com.cozmicgames.audio.Sound
import com.cozmicgames.files.FileHandle
import com.cozmicgames.log
import engine.assets.AssetManager

class SoundManager : StandardAssetTypeManager<Sound, Unit>(Sound::class) {
    override val supportedFormats = Kore.audio.supportedSoundFormats.toSet()

    override val defaultParams = Unit

    override fun add(file: FileHandle, name: String, params: Unit) {
        if (!file.exists) {
            Kore.log.error(this::class, "Sound file not found: $file")
            return
        }

        val sound = Kore.audio.readSound(file)

        if (sound == null) {
            Kore.log.error(this::class, "Failed to load sound file: $file")
            return
        }

        add(name, sound, file)
    }
}

val AssetManager.sounds get() = getAssetTypeManager<Sound>() as? SoundManager

fun AssetManager.getSound(name: String) = getAsset(name, Sound::class)
