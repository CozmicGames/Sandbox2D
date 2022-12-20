package engine.graphics

import com.cozmicgames.graphics.gpu.Texture2D
import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.collections.Resettable
import com.cozmicgames.utils.maths.Rectangle

sealed class Renderable {
    var layer = 0
    var flipX = false
    var flipY = false

    val bounds = Rectangle()

    abstract fun updateBounds()
}

class DrawableRenderable : Renderable() {
    lateinit var material: Material
    lateinit var drawable: Drawable

    override fun updateBounds() {
        bounds.infinite()
        drawable.vertices.forEach {
            bounds.merge(it.x, it.y)
        }
    }
}

class DirectRenderable : Renderable(), Resettable, Disposable {
    companion object {
        private const val CONTEXT_SIZE = 128
    }

    lateinit var texture: Texture2D
    var shader = "default"
    val context = DrawContext(CONTEXT_SIZE)

    override fun updateBounds() {
        bounds.infinite()
    }

    override fun reset() {
        context.reset()
    }

    override fun dispose() {
        context.dispose()
    }
}
