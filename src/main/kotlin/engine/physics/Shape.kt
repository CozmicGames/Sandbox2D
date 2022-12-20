package engine.physics

import com.cozmicgames.utils.extensions.isOdd
import com.cozmicgames.utils.maths.*
import engine.Game
import kotlin.math.min

sealed class Shape {
    abstract fun copy(): Shape
}

class CircleShape : Shape() {
    var radius: Float = 0.5f
    var x = 0.0f
    var y = 0.0f

    override fun copy(): Shape {
        val shape = CircleShape()
        shape.radius = radius
        shape.x = x
        shape.y = y
        return shape
    }
}

class AxisAlignedRectangleShape : Shape() {
    var centerX: Float = 0.0f
    var centerY: Float = 0.0f
    var width: Float = 1.0f
    var height: Float = 1.0f

    val minX get() = centerX - width * 0.5f
    val minY get() = centerY - height * 0.5f
    val maxX get() = centerX + width * 0.5f
    val maxY get() = centerY + height * 0.5f

    override fun copy(): Shape {
        val shape = AxisAlignedRectangleShape()
        shape.centerX = centerX
        shape.centerY = centerY
        shape.width = width
        shape.height = height
        return shape
    }
}

open class PolygonShape : Shape() {
    inner class Vertex(block: Vertex.() -> Unit = {}) {
        val position = Vector2()
        val normal = Vector2()

        var localX by position::x
        var localY by position::y

        var normalX by normal::x
        var normalY by normal::y

        var x
            get() = localX * scale.x
            set(value) {
                localX = value / scale.x
            }

        var y
            get() = localY * scale.y
            set(value) {
                localY = value / scale.y
            }

        val worldX get() = rotation.transformX(x, y) + translation.x
        val worldY get() = rotation.transformY(x, y) + translation.y

        val worldNormalX get() = rotation.transformX(normalX, normalY)
        val worldNormalY get() = rotation.transformY(normalX, normalY)

        init {
            block(this)
        }
    }

    var vertices: Array<Vertex> = emptyArray()
        private set

    val translation = Vector2()
    val scale = Vector2()
    val rotation = Matrix2x2()
    val transposedRotation = Matrix2x2()

    fun setSize(size: Int) {
        if (vertices.size == size) {
            vertices.forEach {
                it.position.setZero()
                it.normal.setZero()
            }
        } else
            vertices = Array(size) { Vertex() }
    }

    fun setFromPoints(vararg points: Vector2) {
        val data = FloatArray(points.size * 2)
        points.forEachIndexed { i, p ->
            data[i * 2] = p.x
            data[i * 2 + 1] = p.y
        }
        setFromPoints(*data)
    }

    fun setFromPoints(vararg data: Float) {
        fun FloatArray.x(i: Int) = this[i * 2]
        fun FloatArray.y(i: Int) = this[i * 2 + 1]

        if (data.size.isOdd || data.size / 2 < 3)
            return

        val count = min(data.size / 2, Game.physics.maxPolygonVertexCount)

        var rightMost = 0
        var highestX = data.x(0)

        for (i in (1 until count)) {
            val x = data.x(i)
            if (x > highestX) {
                highestX = x
                rightMost = i
            } else if (x == highestX)
                if (data.y(i) < data.y(rightMost))
                    rightMost = i
        }

        val hull = IntArray(Game.physics.maxPolygonVertexCount)
        var outCount = 0
        var indexHull = rightMost

        while (true) {
            hull[outCount] = indexHull

            var nextHullIndex = 0
            for (i in (1 until count)) {
                if (nextHullIndex == indexHull) {
                    nextHullIndex = i
                    continue
                }

                val e0x = data.x(nextHullIndex) - data.x(hull[outCount])
                val e0y = data.y(nextHullIndex) - data.y(hull[outCount])
                val e1x = data.x(i) - data.x(hull[outCount])
                val e1y = data.y(i) - data.y(hull[outCount])
                val c = cross(e0x, e0y, e1x, e1y)

                if (c < 0.0f || (c == 0.0f && lengthSquared(e1x, e1y) > lengthSquared(e0x, e0y)))
                    nextHullIndex = i
            }

            outCount++
            indexHull = nextHullIndex

            if (nextHullIndex == rightMost)
                break
        }

        vertices = Array(outCount) {
            Vertex {
                x = data.x(it)
                y = data.y(it)
            }
        }

        for (i0 in vertices.indices) {
            val i1 = if (i0 + 1 < vertices.size) i0 + 1 else 0

            val faceX = vertices[i1].x - vertices[i0].x
            val faceY = vertices[i1].y - vertices[i0].y

            vertices[i0].normalY = faceY
            vertices[i0].normalX = -faceX
            vertices[i0].normal.normalize()
        }
    }

    fun setVertices(vararg vertices: Vertex) {
        this.vertices = arrayOf(*vertices)
    }

    fun getSupport(x: Float, y: Float): Vertex {
        var bestProjection = -Float.MAX_VALUE
        var bestVertex = vertices.first()

        repeat(vertices.size) {
            val v = vertices[it]
            val projection = dot(v.x, v.y, x, y)

            if (projection > bestProjection) {
                bestVertex = v
                bestProjection = projection
            }
        }

        return bestVertex
    }

    override fun copy(): Shape {
        val shape = PolygonShape()
        shape.setSize(vertices.size)
        repeat(vertices.size) {
            shape.vertices[it].position.set(vertices[it].position)
            shape.vertices[it].normal.set(vertices[it].normal)
        }
        shape.translation.set(translation)
        shape.scale.set(scale)
        shape.rotation.set(rotation)
        return shape
    }
}

class RectangleShape(width: Float = 1.0f, height: Float = 1.0f) : PolygonShape() {
    var width = width
        set(value) {
            field = value
            updatePolygonPoints()
        }

    var height = height
        set(value) {
            field = value
            updatePolygonPoints()
        }

    init {
        updatePolygonPoints()
    }

    private fun updatePolygonPoints() {
        val hw = width * 0.5f
        val hh = height * 0.5f

        setSize(4)

        vertices[0].position.set(-hw, -hh)
        vertices[1].position.set(hw, -hh)
        vertices[2].position.set(hw, hh)
        vertices[3].position.set(-hw, hh)

        vertices[0].normal.set(0.0f, -1.0f)
        vertices[1].normal.set(1.0f, 0.0f)
        vertices[2].normal.set(0.0f, 1.0f)
        vertices[3].normal.set(-1.0f, 0.0f)
    }
}
