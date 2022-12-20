package engine.graphics.ui

import com.cozmicgames.utils.collections.Pool
import com.cozmicgames.utils.collections.PriorityList
import com.cozmicgames.utils.maths.Vector2

/**
 * Reports if a point is contained within any area of bounds.
 * This is used to determine if the point of touch is handled by a GUI layer.
 *
 * There is lots to be optimized here, but it works for now.
 */
class GUIVisibility {
    class Node : Comparable<Node> {
        var x = 0.0f
        var y = 0.0f
        var width = 0.0f
        var height = 0.0f

        override fun compareTo(other: Node): Int {
            return if (x < other.x) -1 else if (x > other.x) 1 else 0
        }
    }

    private val nodePool = Pool(supplier = { Node() })
    private val nodesInternal = PriorityList<Node>()

    val nodes get() = nodesInternal.asIterable()

    private var minX = Float.MAX_VALUE
    private var minY = Float.MAX_VALUE
    private var maxX = -Float.MAX_VALUE
    private var maxY = -Float.MAX_VALUE

    fun add(x: Float, y: Float, width: Float, height: Float) {
        nodesInternal.add(nodePool.obtain().also {
            it.x = x
            it.y = y
            it.width = width
            it.height = height
        })

        if (x < minX)
            minX = x

        if (y < minY)
            minY = y

        if (x + width > maxX)
            maxX = x + width

        if (y + height > maxY)
            maxY = y + height
    }

    operator fun contains(point: Vector2) = contains(point.x, point.y)

    fun contains(x: Float, y: Float): Boolean {
        if (nodesInternal.isEmpty())
            return false

        if (x < minX || x > maxX || y < minY || y > maxY)
            return false

        for (node in nodesInternal) {
            if (x < node.x)
                return false

            if (node.x <= x && node.x + node.width >= x && node.y <= y && node.y + node.height >= y)
                return true
        }

        return false
    }

    fun reset() {
        nodesInternal.forEach(nodePool::free)
        nodesInternal.clear()
        minX = 0.0f
        minY = 0.0f
        maxX = 0.0f
        maxY = 0.0f
    }
}