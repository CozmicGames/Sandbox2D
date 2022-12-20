package engine.assets

import com.cozmicgames.files.FileHandle
import com.cozmicgames.files.nameWithExtension
import com.cozmicgames.utils.Disposable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class AssetTypeManager<T : Any, P>(val assetType: KClass<T>) : Disposable {
    inner class Getter(private val file: FileHandle, private val name: String, private val params: P) {
        operator fun getValue(thisRef: Any, property: KProperty<*>) = getOrAdd(file, name, params)
    }

    abstract val supportedFormats: Set<String>

    abstract val defaultParams: P

    abstract val names: Set<String>

    open fun getParams(metaFile: MetaFile): P = defaultParams

    fun load(file: FileHandle) {
        val metaFileHandle = file.sibling("${file.nameWithExtension}.meta")

        if (metaFileHandle.exists) {
            val metaFile = MetaFile()
            metaFile.read(metaFileHandle)

            add(file, metaFile.name, getParams(metaFile))
        } else
            add(file, file.fullPath, defaultParams)
    }

    abstract fun add(file: FileHandle, name: String = file.fullPath, params: P = defaultParams)

    abstract operator fun contains(name: String): Boolean

    abstract fun remove(name: String): Boolean

    abstract operator fun get(name: String): T?

    abstract fun getFileHandle(name: String): FileHandle?

    fun getOrAdd(file: FileHandle, name: String = file.fullPath, params: P = defaultParams): T {
        if (name !in this)
            add(file, name, params)

        return requireNotNull(this[name])
    }

    operator fun invoke(file: FileHandle, name: String = file.fullPath, params: P = defaultParams) = Getter(file, name, params)
}