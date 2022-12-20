package engine.graphics

import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.collections.Pool
import engine.Game
import engine.assets.managers.getTexture

class RenderBatchBuilder : Disposable {
    private val drawBatchPool = Pool(supplier = { RenderBatch() })
    private val batches = arrayListOf<RenderBatch>()

    fun submit(material: Material, drawable: Drawable, flipX: Boolean, flipY: Boolean) {
        var batch = batches.lastOrNull()

        if (batch == null || material != batch.material || flipX != batch.flipX || flipY != batch.flipY) {
            batch = drawBatchPool.obtain()
            batch.material = material
            batch.flipX = flipX
            batch.flipY = flipY
            batches += batch
        }

        val region = Game.assets.getTexture(material.colorTexturePath)
        batch.context.drawDrawable(drawable, region.u0, region.v0, region.u1, region.v1, material.color)
    }

    fun flush(block: (RenderBatch) -> Unit) {
        if (batches.isEmpty())
            return

        batches.forEach {
            block(it)
            drawBatchPool.free(it)
        }
        batches.clear()
    }

    override fun dispose() {
        flush {}
        drawBatchPool.dispose()
    }
}