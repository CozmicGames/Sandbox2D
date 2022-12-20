package engine.assets

import com.cozmicgames.files.FileHandle
import com.cozmicgames.files.readToString
import com.cozmicgames.files.writeString
import com.cozmicgames.utils.Properties
import com.cozmicgames.utils.string

open class MetaFile : Properties() {
    var name by string { "" }

    fun read(file: FileHandle) {
        read(file.readToString())
    }

    fun write(file: FileHandle) {
        file.writeString(write(true), false)
    }
}
