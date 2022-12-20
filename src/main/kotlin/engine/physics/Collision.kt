package engine.physics

import com.cozmicgames.utils.concurrency.threadLocal
import com.cozmicgames.utils.extensions.clamp
import com.cozmicgames.utils.maths.*
import kotlin.math.abs
import kotlin.math.sqrt

object Collision {
    private class Context {
        data class AxisInfo(var distance: Float = 0.0f, var index: Int = 0)

        val tempPolygon = PolygonShape()
        val axisInfo = AxisInfo()
        val incidentFace = Array(2) { Vector2() }
    }

    private val context by threadLocal { Context() }

    fun collideAxisAlignedRectangleToAxisAlignedRectangle(a: AxisAlignedRectangleShape, b: AxisAlignedRectangleShape): Manifold? {
        val midAx = (a.minX + a.maxX) * 0.5f
        val midAy = (a.minY + a.maxY) * 0.5f

        val midBx = (b.minX + b.maxX) * 0.5f
        val midBy = (b.minY + b.maxY) * 0.5f

        val extendAx = abs((a.maxX - a.minX) * 0.5f)
        val extendAy = abs((a.maxY - a.minY) * 0.5f)

        val extendBx = abs((b.maxX - b.minX) * 0.5f)
        val extendBy = abs((b.maxY - b.minY) * 0.5f)

        val normalX = midBx - midAx
        val normalY = midBy - midAy

        val dx = extendAx + extendBx - abs(normalX)
        if (dx <= FLOAT_EPSILON)
            return null

        val dy = extendAy + extendBy - abs(normalY)
        if (dy <= FLOAT_EPSILON)
            return null

        val manifold = Manifold()
        manifold.numContacts = 1

        if (dx < dy) {
            manifold.penetration = dx

            if (normalX < 0.0f) {
                manifold.normal.x = -1.0f
                manifold.normal.y = 0.0f
                manifold.contacts[0].x = midAx - extendAx
                manifold.contacts[0].y = midAy
            } else {
                manifold.normal.x = 1.0f
                manifold.normal.y = 0.0f
                manifold.contacts[0].x = midAx - extendAx
                manifold.contacts[0].y = midAy
            }
        } else {
            manifold.penetration = dy

            if (normalY < 0.0f) {
                manifold.normal.x = 0.0f
                manifold.normal.y = -1.0f
                manifold.contacts[0].x = midAx
                manifold.contacts[0].y = midAy - extendAy
            } else {
                manifold.normal.x = 0.0f
                manifold.normal.y = 1.0f
                manifold.contacts[0].x = midAx
                manifold.contacts[0].y = midAy - extendAy
            }
        }

        return manifold
    }

    fun collideCircleToCircle(a: CircleShape, b: CircleShape): Manifold? {
        val dx = b.x - a.x
        val dy = b.y - a.y

        val dls = lengthSquared(dx, dy)
        val r = a.radius + b.radius

        if (dls < r * r) {
            val manifold = Manifold()
            manifold.numContacts = 1

            val dl = sqrt(dls)

            manifold.penetration = r - dl

            if (dl != 0.0f)
                normalized(dx, dy) { nx, ny ->
                    manifold.contacts[0].x = b.x - nx * b.radius
                    manifold.contacts[0].y = b.y - ny * b.radius
                    manifold.normal.x = nx
                    manifold.normal.y = ny
                }

            return manifold
        }

        return null
    }

    fun collideCircleToAxisAlignedRectangle(a: CircleShape, b: AxisAlignedRectangleShape): Manifold? {
        val lx = a.x.clamp(b.minX, b.maxX)
        val ly = a.y.clamp(b.minY, b.maxY)

        val abx = lx - a.x
        val aby = ly - a.y

        val ds = lengthSquared(abx, aby)
        val rs = a.radius * a.radius

        if (ds < rs) {
            val manifold = Manifold()
            manifold.numContacts = 1

            if (ds != 0.0f) {
                val d = sqrt(ds)
                normalized(abx, aby) { nx, ny ->
                    manifold.penetration = a.radius - d
                    manifold.contacts[0].x = a.x + nx * d
                    manifold.contacts[0].y = a.y + ny * d
                    manifold.normal.x = nx
                    manifold.normal.y = ny
                }
            } else {
                val midX = (b.minX + b.maxX) * 0.5f
                val midY = (b.minY + b.maxY) * 0.5f

                val ex = (b.maxX - b.minX) * 0.5f
                val ey = (b.maxY - b.minY) * 0.5f

                val dx = a.x - midX
                val dy = a.y - midY

                val absDx = abs(dx)
                val absDy = abs(dy)

                val overlapX = ex - absDx
                val overlapY = ey - absDy

                if (overlapX < overlapY) {
                    manifold.penetration = a.radius + overlapX
                    manifold.normal.x = if (dx < 0.0f) 1.0f else -1.0f
                    manifold.normal.y = 0.0f
                } else {
                    manifold.penetration = a.radius + overlapY
                    manifold.normal.x = 0.0f
                    manifold.normal.y = if (dy < 0.0f) 1.0f else -1.0f
                }

                manifold.contacts[0].x = a.x - manifold.normal.x * (manifold.penetration - a.radius)
                manifold.contacts[0].y = a.y - manifold.normal.y * (manifold.penetration - a.radius)
            }

            return manifold
        }

        return null
    }

    fun collideCircleToPolygon(a: CircleShape, b: PolygonShape): Manifold? {
        val cxt = a.x - b.translation.x
        val cyt = a.y - b.translation.y
        val centerX = b.transposedRotation.transformX(cxt, cyt)
        val centerY = b.transposedRotation.transformY(cxt, cyt)

        var separation = -Float.MAX_VALUE
        var faceNormal = 0

        repeat(b.vertices.size) {
            val v = b.vertices[it]
            val s = dot(v.normalX, v.normalY, centerX - v.x, centerY - v.y)

            if (s > a.radius)
                return null

            if (s > separation) {
                separation = s
                faceNormal = it
            }
        }

        val manifold = Manifold()

        if (separation < FLOAT_EPSILON) {
            val v = b.vertices[faceNormal]

            manifold.numContacts = 1
            manifold.normal.x = b.rotation.transformX(v.normalX, v.normalY)
            manifold.normal.y = b.rotation.transformY(v.normalX, v.normalY)
            manifold.contacts[0].x = manifold.normal.x * a.radius + a.x
            manifold.contacts[0].y = manifold.normal.y * a.radius + a.y
            manifold.penetration = a.radius
            return manifold
        }

        val v0 = b.vertices[faceNormal].position
        val v1 = b.vertices[if (faceNormal + 1 < b.vertices.size) faceNormal + 1 else 0].position

        val dot0 = dot(centerX - v0.x, centerY - v0.y, v1.x - v0.x, v1.y - v0.y)
        val dot1 = dot(centerX - v1.x, centerY - v1.y, v0.x - v1.x, v0.y - v1.y)

        manifold.penetration = a.radius - separation

        when {
            dot0 <= 0.0f -> {
                if (distanceSquared(centerX, centerY, v0.x, v0.y) > a.radius * a.radius)
                    return null

                manifold.numContacts = 1
                val nx = v0.x - centerX
                val ny = v0.y - centerY

                manifold.normal.x = b.rotation.transformX(nx, ny)
                manifold.normal.y = b.rotation.transformY(nx, ny)
                manifold.normal.normalize()

                val vx = v0.x + b.translation.x
                val vy = v0.y + b.translation.y

                manifold.contacts[0].x = b.rotation.transformX(vx, vy)
                manifold.contacts[0].y = b.rotation.transformY(vx, vy)
            }
            dot1 <= 0.0f -> {
                if (distanceSquared(centerX, centerY, v1.x, v1.y) > a.radius * a.radius)
                    return null

                manifold.numContacts = 1
                val nx = v1.x - centerX
                val ny = v1.y - centerY

                manifold.normal.x = b.rotation.transformX(nx, ny)
                manifold.normal.y = b.rotation.transformY(nx, ny)
                manifold.normal.normalize()

                val vx = v1.x + b.translation.x
                val vy = v1.y + b.translation.y

                manifold.contacts[0].x = b.rotation.transformX(vx, vy)
                manifold.contacts[0].y = b.rotation.transformY(vx, vy)
            }
            else -> {
                val nx = b.vertices[faceNormal].normalX
                val ny = b.vertices[faceNormal].normalY

                if (dot(centerX - v0.x, centerY - v0.y, nx, ny) > a.radius)
                    return null

                manifold.numContacts = 1

                manifold.normal.x = -b.rotation.transformX(nx, ny)
                manifold.normal.y = -b.rotation.transformY(nx, ny)

                manifold.contacts[0].x = manifold.normal.x * a.radius + a.x
                manifold.contacts[0].y = manifold.normal.y * a.radius + a.y
            }
        }

        return manifold
    }

    fun collideAxisAlignedRectangleToPolygon(a: AxisAlignedRectangleShape, b: PolygonShape): Manifold? {
        val ap = context.tempPolygon

        ap.setSize(4)

        ap.vertices[0].position.set(a.minX, a.minY)
        ap.vertices[1].position.set(a.maxX, a.minY)
        ap.vertices[2].position.set(a.maxX, a.maxY)
        ap.vertices[3].position.set(a.minX, a.maxY)

        ap.vertices[0].normal.set(0.0f, -1.0f)
        ap.vertices[1].normal.set(1.0f, 0.0f)
        ap.vertices[2].normal.set(0.0f, 1.0f)
        ap.vertices[3].normal.set(-1.0f, 0.0f)

        return collidePolygonToPolygon(ap, b)
    }

    fun collidePolygonToPolygon(a: PolygonShape, b: PolygonShape): Manifold? {
        val context = context

        fun findAxisLeastPenetration(a: PolygonShape, b: PolygonShape): Context.AxisInfo {
            var bestDistance = -Float.MAX_VALUE
            var bestIndex = 0

            repeat(a.vertices.size) {
                var nx = a.vertices[it].normalX
                var ny = a.vertices[it].normalY

                val nwx = a.rotation.transformX(nx, ny)
                val nwy = a.rotation.transformY(nx, ny)

                nx = b.transposedRotation.transformX(nwx, nwy)
                ny = b.transposedRotation.transformY(nwx, nwy)

                val s = b.getSupport(-nx, -ny)

                val vxt = a.vertices[it].x
                val vyt = a.vertices[it].y

                val vxtt = a.rotation.transformX(vxt, vyt) + a.translation.x - b.translation.x
                val vytt = a.rotation.transformY(vxt, vyt) + a.translation.y - b.translation.y

                val vx = b.transposedRotation.transformX(vxtt, vytt)
                val vy = b.transposedRotation.transformY(vxtt, vytt)

                val d = dot(nx, ny, s.x - vx, s.y - vy)

                if (d > bestDistance) {
                    bestDistance = d
                    bestIndex = it
                }
            }

            context.axisInfo.distance = bestDistance
            context.axisInfo.index = bestIndex
            return context.axisInfo
        }

        fun findIncidentFace(ref: PolygonShape, inc: PolygonShape, referenceIndex: Int, v: Array<Vector2>) {
            val rnxt = ref.vertices[referenceIndex].normalX
            val rnyt = ref.vertices[referenceIndex].normalY

            val rnxtt = ref.rotation.transformX(rnxt, rnyt)
            val rnytt = ref.rotation.transformY(rnxt, rnyt)

            val rnx = inc.transposedRotation.transformX(rnxtt, rnytt)
            val rny = inc.transposedRotation.transformY(rnxtt, rnytt)

            var incidentFace = 0
            var minDot = Float.MAX_VALUE

            repeat(inc.vertices.size) {
                val vert = inc.vertices[it]
                val dot = dot(rnx, rny, vert.normalX, vert.normalY)

                if (dot < minDot) {
                    minDot = dot
                    incidentFace = it
                }
            }

            val incV0 = inc.vertices[incidentFace]
            val incV1 = inc.vertices[if (incidentFace + 1 >= inc.vertices.size) 0 else incidentFace + 1]

            v[0].x = inc.rotation.transformX(incV0.x, incV0.y) + inc.translation.x
            v[0].y = inc.rotation.transformY(incV0.x, incV0.y) + inc.translation.y

            v[1].x = inc.rotation.transformX(incV1.x, incV1.y) + inc.translation.x
            v[1].y = inc.rotation.transformY(incV1.x, incV1.y) + inc.translation.y
        }

        fun clip(nx: Float, ny: Float, c: Float, face: Array<Vector2>): Int {
            var sp = 0
            val out = arrayOf(face[0].copy(), face[1].copy())

            val d0 = dot(nx, ny, face[0].x, face[0].y) - c
            val d1 = dot(nx, ny, face[1].x, face[1].y) - c

            if (d0 <= 0.0f)
                out[sp++].set(face[0])

            if (d1 <= 0.0f)
                out[sp++].set(face[1])

            if (d0 * d1 < 0.0f) {
                val alpha = d0 / (d0 - d1)
                out[sp].x = face[0].x + alpha * (face[1].x - face[0].x)
                out[sp].y = face[0].y + alpha * (face[1].y - face[0].y)
                sp++
            }

            face[0].set(out[0])
            face[1].set(out[1])

            require(sp != 3)

            return sp
        }

        fun biasGreaterThan(a: Float, b: Float): Boolean {
            val biasRelative = 0.95f
            val biasAbsolute = 0.01f
            return a >= b * biasRelative + a * biasAbsolute
        }

        val (penetrationA, faceA) = findAxisLeastPenetration(a, b)
        if (penetrationA >= 0.0f)
            return null

        val (penetrationB, faceB) = findAxisLeastPenetration(b, a)
        if (penetrationB >= 0.0f)
            return null

        val referenceIndex: Int
        val flip: Boolean

        val ref: PolygonShape
        val inc: PolygonShape

        if (biasGreaterThan(penetrationA, penetrationB)) {
            ref = a
            inc = b
            referenceIndex = faceA
            flip = false
        } else {
            ref = b
            inc = a
            referenceIndex = faceB
            flip = true
        }

        val incidentFace = context.incidentFace

        findIncidentFace(ref, inc, referenceIndex, incidentFace)

        val v0v = ref.vertices[referenceIndex]
        val v1v = ref.vertices[if (referenceIndex + 1 >= ref.vertices.size) 0 else referenceIndex + 1]

        val v0 = Vector2(v0v.x, v0v.y)
        val v1 = Vector2(v1v.x, v1v.y)

        ref.rotation.transform(v0)
        v0.x += ref.translation.x
        v0.y += ref.translation.y

        ref.rotation.transform(v1)
        v1.x += ref.translation.x
        v1.y += ref.translation.y

        val sidePlaneNormal = Vector2(v1.x - v0.x, v1.y - v0.y)
        sidePlaneNormal.normalize()

        val refFaceNormal = Vector2(sidePlaneNormal.y, -sidePlaneNormal.x)

        val refC = dot(refFaceNormal.x, refFaceNormal.y, v0.x, v0.y)
        val negSide = -dot(sidePlaneNormal.x, sidePlaneNormal.y, v0.x, v0.y)
        val posSide = dot(sidePlaneNormal.x, sidePlaneNormal.y, v1.x, v1.y)

        if (clip(-sidePlaneNormal.x, -sidePlaneNormal.y, negSide, incidentFace) < 2)
            return null

        if (clip(sidePlaneNormal.x, sidePlaneNormal.y, posSide, incidentFace) < 2)
            return null

        val manifold = Manifold()
        manifold.normal.x = if (flip) -refFaceNormal.x else refFaceNormal.x
        manifold.normal.y = if (flip) -refFaceNormal.y else refFaceNormal.y

        var cp = 0
        var separation = dot(refFaceNormal.x, refFaceNormal.y, incidentFace[0].x, incidentFace[0].y) - refC
        if (separation <= 0.0f) {
            manifold.contacts[cp].set(incidentFace[0])
            manifold.penetration = -separation
            cp++
        } else
            manifold.penetration = 0.0f

        separation = dot(refFaceNormal.x, refFaceNormal.y, incidentFace[1].x, incidentFace[1].y) - refC
        if (separation <= 0.0f) {
            manifold.contacts[cp].set(incidentFace[1])
            manifold.penetration += -separation
            cp++
            manifold.penetration /= cp
        }

        manifold.numContacts = cp

        return manifold
    }
}