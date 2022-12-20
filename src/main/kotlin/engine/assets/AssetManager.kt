package engine.assets

import com.cozmicgames.Kore
import com.cozmicgames.files
import com.cozmicgames.files.*
import com.cozmicgames.log
import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.extensions.extension
import engine.assets.managers.*
import kotlin.reflect.KClass

class AssetManager : Disposable {
    private val registeredManagers = hashMapOf<Any, AssetTypeManager<*, *>>()

    val managers get() = registeredManagers.values.toSet()

    init {
        registerAssetTypeManager(TextureManager())
        registerAssetTypeManager(FontManager())
        registerAssetTypeManager(SoundManager())
        registerAssetTypeManager(MaterialManager())
        registerAssetTypeManager(TileSetManager())
        registerAssetTypeManager(ShaderManager())
    }

    fun toAssetFileHandle(name: String) = Kore.files.local("assets/$name")

    inline fun <reified T : Any> registerAssetTypeManager(manager: AssetTypeManager<T, *>) = registerAssetTypeManager(T::class, manager)

    fun <T : Any> registerAssetTypeManager(type: KClass<T>, manager: AssetTypeManager<T, *>) {
        registeredManagers.put(type, manager)?.dispose()
    }

    inline fun <reified T : Any> getAssetTypeManager() = getAssetTypeManager(T::class)

    fun <T : Any> getAssetTypeManager(type: KClass<T>) = registeredManagers[type] as? AssetTypeManager<T, *>

    fun findAssetTypeManager(predicate: (AssetTypeManager<*, *>) -> Boolean): AssetTypeManager<*, *>? {
        registeredManagers.forEach { (_, manager) ->
            if (predicate(manager))
                return manager
        }

        return null
    }

    inline fun <reified T : Any> getAsset(name: String) = getAsset(name, T::class)

    fun <T : Any> getAsset(name: String, type: KClass<T>): T? {
        val manager = getAssetTypeManager(type) ?: return null
        return manager[name]
    }

    fun load(file: FileHandle) {
        val manager = findAssetTypeManager { file.extension in it.supportedFormats }

        if (manager == null) {
            Kore.log.error(this::class, "Failed to load '$file', no suitable asset type manager found that could load an asset of type '${file.extension}'.")
            return
        }

        Kore.log.info(this::class, "Loading asset: $file")
        manager.load(file)
    }

    fun createZipArchive() {
        val zipFile = Kore.files.local("assets.zip")

        if (zipFile.exists)
            zipFile.delete()

        zipFile.buildZip {
            fun addDirectory(directoryFile: FileHandle) {
                directoryFile.list {
                    val file = directoryFile.child(it)
                    if (file.isDirectory)
                        addDirectory(file)
                    else {
                        val name = file.fullPath.removePrefix("${directoryFile.fullPath}/")
                        val content = file.readToBytes()

                        addFile(name, content)
                    }
                }
            }

            addDirectory(Kore.files.local("assets"))
        }
    }

    override fun dispose() {
        registeredManagers.forEach { (_, manager) ->
            manager.dispose()
        }
    }
}

fun AssetManager.getAssetFileHandle(name: String): FileHandle? {
    val manager = findAssetTypeManager { name.extension in it.supportedFormats } ?: return null
    return manager.getFileHandle(name)
}

inline fun <reified T : Any> AssetManager.getAssetNames() = getAssetNames(T::class)

fun <T : Any> AssetManager.getAssetNames(type: KClass<T>) = getAssetTypeManager(type)?.names ?: emptySet()

fun AssetManager.remove(name: String): Boolean {
    val manager = findAssetTypeManager { name.extension in it.supportedFormats } ?: return false
    return manager.remove(name)
}
