package engine.utils

import com.cozmicgames.utils.Properties

interface Savable {
    fun read(properties: Properties)
    fun write(properties: Properties)
}