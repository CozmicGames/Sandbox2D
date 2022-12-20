package engine.pathfinding

import com.cozmicgames.utils.maths.Vector2i

class Path {
    val length get() = steps.size

    private val steps = arrayListOf<Vector2i>()

    fun prependStep(x: Int, y: Int) {
        steps.add(0, Vector2i(x, y))
    }

    fun appendStep(x: Int, y: Int) {
        steps += Vector2i(x, y)
    }

    fun getStep(index: Int) = steps[index]

    inline operator fun get(index: Int) = getStep(index)

    fun contains(x: Int, y: Int) = steps.find { it.x == x && it.y == y } != null
}

operator fun Path.contains(point: Vector2i) = contains(point.x, point.y)
