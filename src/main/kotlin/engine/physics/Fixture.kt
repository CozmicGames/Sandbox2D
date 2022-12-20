@file:Suppress("UnnecessaryVariable", "UnnecessaryVariable")

package engine.physics

import com.cozmicgames.utils.maths.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

sealed class Fixture<S : Shape>(val shape: S, val density: Float) {
    lateinit var body: Body

    abstract fun update()

    abstract fun calculateMassAndInertia()

    abstract fun contains(x: Float, y: Float): Boolean

    abstract fun raycast(ray: Ray): RaycastResult?

    abstract fun overlapsCircle(x: Float, y: Float, radius: Float): Boolean

    abstract fun overlapsRectangle(x: Float, y: Float, width: Float, height: Float): Boolean
}

operator fun Fixture<*>.contains(point: Vector2) = contains(point.x, point.y)

class CircleFixture(shape: CircleShape, density: Float) : Fixture<CircleShape>(shape.copy() as CircleShape, density) {
    override fun update() {
        shape.x = body.positionX
        shape.y = body.positionY
        shape.radius = max(body.scaleX, body.scaleY) * 0.5f

        body.bounds.merge(shape.x - shape.radius, shape.y - shape.radius)
        body.bounds.merge(shape.x + shape.radius, shape.y + shape.radius)
    }

    override fun calculateMassAndInertia() {
        if (density > 0.0f)
            body.mass += PI * shape.radius * shape.radius * density

        body.inertia += body.mass * shape.radius * shape.radius
    }

    override fun contains(x: Float, y: Float): Boolean {
        return intersectPointCircle(x, y, shape.x, shape.y, shape.radius)
    }

    override fun raycast(ray: Ray): RaycastResult? {
        val mx = ray.origin.x - shape.x
        val my = ray.origin.y - shape.y
        val c = dot(mx, my, mx, my) - shape.radius * shape.radius
        val b = dot(mx, my, ray.direction.x, ray.direction.y)
        val disc = b * b - c

        if (disc < 0.0f)
            return null

        val t = -b - sqrt(disc)

        if (t >= 0.0f && t <= ray.length) {
            val normal = Vector2()
            normal.x = ray.origin.x + ray.direction.x * t - shape.x
            normal.y = ray.origin.y + ray.direction.y * t - shape.y
            normal.normalize()
            return RaycastResult(ray, t, normal, body)
        }

        return null
    }

    override fun overlapsCircle(x: Float, y: Float, radius: Float): Boolean {
        return intersectCircleCircle(shape.x, shape.y, shape.radius, x, y, radius)
    }

    override fun overlapsRectangle(x: Float, y: Float, width: Float, height: Float): Boolean {
        return intersectRectCirlce(x, y, x + width, y + width, shape.x, shape.y, shape.radius)
    }
}

@Suppress("UnnecessaryVariable", "UnnecessaryVariable", "UnnecessaryVariable", "UnnecessaryVariable", "UnnecessaryVariable", "UnnecessaryVariable", "UnnecessaryVariable", "UnnecessaryVariable")
class AxisAlignedRectangleFixture(shape: AxisAlignedRectangleShape, density: Float) : Fixture<AxisAlignedRectangleShape>(shape.copy() as AxisAlignedRectangleShape, density) {
    override fun update() {
        shape.centerX = body.positionX
        shape.centerY = body.positionY
        shape.width = body.scaleX
        shape.height = body.scaleY

        body.bounds.merge(shape.minX, shape.minY)
        body.bounds.merge(shape.maxX, shape.maxY)
    }

    override fun calculateMassAndInertia() {
        if (density > 0.0f)
            body.mass += shape.width * shape.height * density
    }

    override fun contains(x: Float, y: Float): Boolean {
        return intersectPointRect(x, y, shape.minX, shape.minY, shape.maxX, shape.maxY)
    }

    override fun raycast(ray: Ray): RaycastResult? {
        val p0x = ray.origin.x
        val p0y = ray.origin.y

        val p1x = ray.origin.x + ray.direction.x * ray.length
        val p1y = ray.origin.y + ray.direction.y * ray.length

        val aMinX = min(p0x, p1x)
        val aMinY = min(p0y, p1y)
        val aMaxX = min(p0x, p1x)
        val aMaxY = min(p0y, p1y)

        if (!intersectRectRect(aMinX, aMinY, aMaxX, aMaxY, shape.minX, shape.minY, shape.maxX, shape.maxY))
            return null

        val abx = p1x - p0x
        val aby = p1y - p0y

        val nx = -aby
        val ny = abx

        val absNx = abs(nx)
        val absNy = abs(ny)

        val halfExtentsX = (shape.maxX - shape.minX) * 0.5f
        val halfExtentsY = (shape.maxY - shape.minY) * 0.5f

        val centerX = shape.minX + halfExtentsX
        val centerY = shape.minY + halfExtentsY

        val d = abs(dot(nx, ny, p0x - centerX, p0y - centerY)) - dot(absNx, absNy, halfExtentsX, halfExtentsY)

        if (d > 0.0f)
            return null

        fun signedDistancePointToPlaneOneDimensional(p: Float, n: Float, d: Float) = p * n - d * n

        fun rayToPlaneOneDimensional(da: Float, db: Float) = when {
            da < 0.0f -> 0.0f
            da * db > 0.0f -> 1.0f
            else -> {
                val dd = da - db
                if (dd != 0.0f)
                    da / dd
                else
                    0.0f
            }
        }

        val da0 = signedDistancePointToPlaneOneDimensional(p0x, -1.0f, shape.minX)
        val db0 = signedDistancePointToPlaneOneDimensional(p1x, -1.0f, shape.minX)
        val da1 = signedDistancePointToPlaneOneDimensional(p0x, 1.0f, shape.maxX)
        val db1 = signedDistancePointToPlaneOneDimensional(p1x, 1.0f, shape.maxX)
        val da2 = signedDistancePointToPlaneOneDimensional(p0y, -1.0f, shape.minY)
        val db2 = signedDistancePointToPlaneOneDimensional(p1y, -1.0f, shape.minY)
        val da3 = signedDistancePointToPlaneOneDimensional(p0y, 1.0f, shape.maxY)
        val db3 = signedDistancePointToPlaneOneDimensional(p1y, 1.0f, shape.maxY)
        var t0 = rayToPlaneOneDimensional(da0, db0)
        var t1 = rayToPlaneOneDimensional(da1, db1)
        var t2 = rayToPlaneOneDimensional(da2, db2)
        var t3 = rayToPlaneOneDimensional(da3, db3)

        val hit0 = t0 < 1.0f
        val hit1 = t1 < 1.0f
        val hit2 = t2 < 1.0f
        val hit3 = t3 < 1.0f
        val hit = hit0 || hit1 || hit2 || hit3

        if (hit) {
            t0 = if (hit0) 1.0f else 0.0f * t0
            t1 = if (hit1) 1.0f else 0.0f * t1
            t2 = if (hit2) 1.0f else 0.0f * t2
            t3 = if (hit3) 1.0f else 0.0f * t3

            return if (t0 >= t1 && t0 >= t2 && t0 >= t3)
                RaycastResult(ray, t0 * ray.length, Vector2(-1.0f, 0.0f), body)
            else if (t1 >= t0 && t1 >= t2 && t1 >= t3)
                RaycastResult(ray, t1 * ray.length, Vector2(1.0f, 0.0f), body)
            else if (t2 >= t0 && t2 >= t1 && t2 >= t3)
                RaycastResult(ray, t2 * ray.length, Vector2(0.0f, -1.0f), body)
            else
                RaycastResult(ray, t3 * ray.length, Vector2(0.0f, 1.0f), body)
        }

        return null
    }

    override fun overlapsCircle(x: Float, y: Float, radius: Float): Boolean {
        return intersectRectCirlce(shape.minX, shape.minY, shape.maxX, shape.maxY, x, y, radius)
    }

    override fun overlapsRectangle(x: Float, y: Float, width: Float, height: Float): Boolean {
        return intersectRectRect(x, y, x + width, y + height, shape.minX, shape.minY, shape.maxX, shape.maxY)
    }
}

open class PolygonFixture(shape: PolygonShape, density: Float) : Fixture<PolygonShape>(shape.copy() as PolygonShape, density) {
    override fun update() {
        shape.translation.set(body.positionX, body.positionY)
        shape.scale.set(body.scaleX, body.scaleY)
        shape.rotation.setRotation(body.rotation)
        shape.transposedRotation.set(shape.rotation).transpose()

        shape.vertices.forEach {
            body.bounds.merge(it.worldX, it.worldY)
        }
    }

    override fun calculateMassAndInertia() {
        var area = 0.0f
        var inertia = 0.0f

        repeat(shape.vertices.size) {
            val p0x = shape.vertices[it].x
            val p0y = shape.vertices[it].y
            val i2 = if (it + 1 < shape.vertices.size) it + 1 else 0
            val p1x = shape.vertices[i2].x
            val p1y = shape.vertices[i2].y

            val d = cross(p0x, p0y, p1x, p1y)

            val triangleArea = d * 0.5f

            area += triangleArea

            val intx2 = p0x * p0x + p1x * p0x + p1x * p1x
            val inty2 = p0y * p0y + p1y * p0y + p1y * p1y

            inertia += (0.25f / 3.0f * d) * (intx2 + inty2)
        }

        body.mass += density * area
        body.inertia += density * inertia
    }

    override fun contains(x: Float, y: Float): Boolean {
        repeat(shape.vertices.size) {
            val v = shape.vertices[it]
            if (dot(v.worldNormalX, v.worldNormalY, x - v.x, y - v.y) > 0.0f)
                return false
        }

        return true
    }

    override fun raycast(ray: Ray): RaycastResult? {
        var lo = 0.0f
        var hi = ray.length

        var index = 0.inv()

        for (i in shape.vertices.indices) {
            val num = dot(shape.vertices[i].worldNormalX, shape.vertices[i].worldNormalY, shape.vertices[i].worldX - ray.origin.x, shape.vertices[i].worldY - ray.origin.y)
            val den = dot(shape.vertices[i].worldNormalX, shape.vertices[i].worldNormalY, ray.direction.x, ray.direction.y)

            if (den == 0.0f && num < 0.0f)
                return null
            else {
                if (den < 0.0f && num < lo * den) {
                    lo = num / den
                    index = i
                } else if (den > 0.0f && num < hi * den)
                    hi = num / den
            }

            if (hi < lo)
                return null
        }

        if (index != 0.inv()) {
            return RaycastResult(ray, lo, shape.vertices[index].normal.copy(), body)
        }

        return null
    }

    override fun overlapsCircle(x: Float, y: Float, radius: Float): Boolean {
        return false
    }

    override fun overlapsRectangle(x: Float, y: Float, width: Float, height: Float): Boolean {
        return false
    }
}
