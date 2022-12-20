package engine.scene.components

import com.cozmicgames.utils.Properties
import engine.graphics.Drawable
import engine.scene.Component

class DrawableProviderComponent : Component() {
    val drawables = arrayListOf<Drawable>()

    var isFlippedX = false
    var isFlippedY = false

    override fun read(properties: Properties) {
        properties.getBoolean("isFlippedX")?.let { isFlippedX = it }
        properties.getBoolean("isFlippedY")?.let { isFlippedY = it }
    }

    override fun write(properties: Properties) {
        properties.setBoolean("isFlippedX", isFlippedX)
        properties.setBoolean("isFlippedY", isFlippedY)
    }
}