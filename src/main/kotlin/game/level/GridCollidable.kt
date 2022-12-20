package game.level

import com.cozmicgames.Kore
import com.cozmicgames.utils.Updateable
import com.cozmicgames.utils.collections.ReclaimingPool
import com.cozmicgames.utils.injector
import com.cozmicgames.utils.maths.Rectangle

interface GridCollidable {
    companion object {
        val rectanglePool by Kore.context.injector(true) { BoundsPool() }
    }

    val bounds: Rectangle
        get() {
            val rectangle = rectanglePool.obtain()
            calculateBounds(rectangle)
            return rectangle
        }

    fun calculateBounds(bounds: Rectangle)

    fun onCollision(other: GridCollidable) {}
}

class BoundsPool : ReclaimingPool<Rectangle>(supplier = { Rectangle() }, reset = { it.infinite() }), Updateable {
    override fun update(delta: Float) {
        freePooled()
    }
}
