package engine.scene.animation

import com.cozmicgames.utils.Properties
import com.cozmicgames.utils.UUID

class NodeAnimation {
    var uuid: UUID? = null
    val frames = arrayListOf<AnimationFrame>()

    fun getFrameBefore(time: Float): AnimationFrame? {
        for (i in frames.indices) {
            if (frames[i].time > time)
                return if (i > 0) frames[i - 1] else null
        }
        return null
    }

    fun getFrameAfter(time: Float): AnimationFrame? {
        for (i in frames.indices) {
            if (frames[i].time > time)
                return if (i < frames.size - 1) frames[i + 1] else null
        }
        return null
    }

    fun read(properties: Properties) {
        frames.clear()

        properties.getString("uuid")?.let { uuid = UUID(it) }

        val framesProperties = properties.getPropertiesArray("frames") ?: return

        for (frameProperties in framesProperties) {
            val frame = AnimationFrame()

            frameProperties.getFloat("x")?.let { frame.x = it }
            frameProperties.getFloat("y")?.let { frame.y = it }
            frameProperties.getFloat("rotation")?.let { frame.rotation = it }
            frameProperties.getFloat("scaleX")?.let { frame.scaleX = it }
            frameProperties.getFloat("scaleY")?.let { frame.scaleY = it }

            frames += frame
        }
    }

    fun write(properties: Properties) {
        val uuid = uuid ?: return

        properties.setString("uuid", uuid.toString())

        val framesProperties = arrayListOf<Properties>()

        frames.forEach {
            val frameProperties = Properties()

            frameProperties.setFloat("x", it.x)
            frameProperties.setFloat("y", it.y)
            frameProperties.setFloat("rotation", it.rotation)
            frameProperties.setFloat("scaleX", it.scaleX)
            frameProperties.setFloat("scaleY", it.scaleY)

            framesProperties += frameProperties
        }

        properties.setPropertiesArray("frames", framesProperties.toTypedArray())
    }
}