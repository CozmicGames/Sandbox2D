package engine.graphics

import com.cozmicgames.utils.maths.Rectangle
import com.cozmicgames.utils.maths.VectorPath

class Bounds {
    val path = VectorPath()
    val rectangle = Rectangle()

    fun update(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        path.clear()
        path.add(x0, y0)
        path.add(x1, y1)
        path.add(x2, y2)
        path.add(x3, y3)

        rectangle.x = path.minX
        rectangle.y = path.minY
        rectangle.width = path.maxX - path.minX
        rectangle.height = path.maxY - path.minY
    }
}