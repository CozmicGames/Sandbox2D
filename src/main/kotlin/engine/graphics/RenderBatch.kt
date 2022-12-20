package engine.graphics

import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.collections.Resettable

class RenderBatch : Resettable, Disposable {
    var material: Material? = null
    var flipX = false
    var flipY = false
    val context = DrawContext()

    override fun reset() {
        material = null
        flipX = false
        flipY = false
        context.reset()
    }

    override fun dispose() {
        context.dispose()
    }
}