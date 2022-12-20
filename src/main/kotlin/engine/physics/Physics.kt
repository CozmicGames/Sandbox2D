package engine.physics

import com.cozmicgames.utils.Updateable
import com.cozmicgames.utils.maths.*
import kotlin.math.*

class Physics : Updateable {
    val gravity = Vector2(0.0f, -100.0f)
    var resolveIterations = 100
    var subSteps = 5
    var positionRoundingThreshold = 0.05f
    var doPositionalCorrection = true
    var doPositionRounding = true
    var maxPolygonVertexCount = 20
    var collisionFilter: CollisionFilter? = null
    var collisionListener: CollisionListener? = null

    private val bodies = arrayListOf<Body>()
    private val collisionPairs = arrayListOf<CollisionPair>()
    private val manifolds = arrayListOf<Manifold>()

    override fun update(delta: Float) {
        repeat(subSteps) {
            step(delta / subSteps)
        }
    }

    private fun step(delta: Float) {
        fun integrateForces(body: Body) {
            if (!body.isStatic) {
                body.velocity.x += (body.force.x * body.inverseMass + body.gravityScale * gravity.x) * delta * 0.5f
                body.velocity.y += (body.force.y * body.inverseMass + body.gravityScale * gravity.y) * delta * 0.5f
            }

            if (!body.isRotationLocked)
                body.angularVelocity += body.torque * body.inverseInertia * delta * 0.5f
        }

        fun integrateVelocity(body: Body) {
            if (!body.isStatic) {
                body.positionX += body.velocity.x * delta
                body.positionY += body.velocity.y * delta
            }

            if (!body.isRotationLocked)
                body.rotation += body.angularVelocity * delta

            integrateForces(body)
        }

        fun initializeManifold(manifold: Manifold) = with(manifold) {
            restitution = min(a.restitution, b.restitution)
            staticFriction = sqrt(a.staticFriction * b.staticFriction)
            dynamicFriction = sqrt(a.dynamicFriction * b.dynamicFriction)

            repeat(numContacts) {
                val rax = contacts[it].x - a.positionX
                val ray = contacts[it].y - a.positionY

                val rbx = contacts[it].x - b.positionX
                val rby = contacts[it].y - b.positionY

                val rvx = b.velocity.x + cross(rbx, rby, b.angularVelocity) { x, y -> x } - a.velocity.x - cross(rax, ray, a.angularVelocity) { x, y -> x }
                val rvy = b.velocity.y + cross(rbx, rby, b.angularVelocity) { x, y -> y } - a.velocity.y - cross(rax, ray, a.angularVelocity) { x, y -> y }

                if (lengthSquared(rvx, rvy) < lengthSquared(gravity.x * delta, gravity.y * delta) + FLOAT_EPSILON)
                    restitution = 0.0f
            }
        }

        fun resolveCollision(manifold: Manifold) = with(manifold) {
            if (a.isStatic && b.isStatic) {
                a.velocity.setZero()
                b.velocity.setZero()
                return
            }

            repeat(numContacts) {
                val rax = contacts[it].x - a.positionX
                val ray = contacts[it].y - a.positionY

                val rbx = contacts[it].x - b.positionX
                val rby = contacts[it].y - b.positionY

                var rvx = b.velocity.x + cross(rbx, rby, b.angularVelocity) { x, y -> x } - a.velocity.x - cross(rax, ray, a.angularVelocity) { x, y -> x }
                var rvy = b.velocity.y + cross(rbx, rby, b.angularVelocity) { x, y -> y } - a.velocity.y - cross(rax, ray, a.angularVelocity) { x, y -> y }

                val contactVelocity = dot(rvx, rvy, normal.x, normal.y)

                if (contactVelocity > 0.0f)
                    return

                val raCrossN = cross(rax, ray, normal.x, normal.y)
                val rbCrossN = cross(rbx, rby, normal.x, normal.y)

                val invMassSum = a.inverseMass + b.inverseMass + ((raCrossN * raCrossN) * a.inverseInertia) + ((rbCrossN * rbCrossN) * b.inverseInertia)

                var j = -(1.0f + restitution) * contactVelocity
                j /= invMassSum
                j /= numContacts

                val impulseX = normal.x * j
                val impulseY = normal.y * j

                a.applyImpulse(-impulseX, -impulseY, rax, ray)
                b.applyImpulse(impulseX, impulseY, rbx, rby)

                rvx = b.velocity.x + cross(rbx, rby, b.angularVelocity) { x, y -> x } - a.velocity.x - cross(rax, ray, a.angularVelocity) { x, y -> x }
                rvy = b.velocity.y + cross(rbx, rby, b.angularVelocity) { x, y -> y } - a.velocity.y - cross(rax, ray, a.angularVelocity) { x, y -> y }

                var tangentX = rvx - (normal.x * dot(rvx, rvy, normal.x, normal.y))
                var tangentY = rvy - (normal.y * dot(rvx, rvy, normal.x, normal.y))
                val tangentLength = length(tangentX, tangentY)

                if (tangentLength > FLOAT_EPSILON) {
                    tangentX /= tangentLength
                    tangentY /= tangentLength
                }

                var jt = -dot(rvx, rvy, tangentX, tangentY)
                jt /= invMassSum
                jt /= numContacts

                if (abs(jt) < FLOAT_EPSILON)
                    return

                val tangentImpulseX: Float
                val tangentImpulseY: Float

                if (abs(jt) < j * staticFriction) {
                    tangentImpulseX = tangentX * jt
                    tangentImpulseY = tangentY * jt
                } else {
                    tangentImpulseX = tangentX * -j * dynamicFriction
                    tangentImpulseY = tangentY * -j * dynamicFriction
                }

                a.applyImpulse(-tangentImpulseX, -tangentImpulseY, rax, ray)
                b.applyImpulse(tangentImpulseX, tangentImpulseY, rbx, rby)
            }
        }

        fun positionalCorrection(manifold: Manifold) = with(manifold) {
            val slop = 0.05f
            val percent = 0.4f

            val correctionX = (max(penetration - slop, 0.0f) / (a.inverseMass + b.inverseMass)) * normal.x * percent
            val correctionY = (max(penetration - slop, 0.0f) / (a.inverseMass + b.inverseMass)) * normal.y * percent

            a.positionX -= correctionX * a.inverseMass
            a.positionY -= correctionY * a.inverseMass
            b.positionX += correctionX * b.inverseMass
            b.positionY += correctionY * b.inverseMass
        }

        manifolds.clear()
        collisionPairs.clear()

        for (i in (0 until bodies.size)) {
            val a = bodies[i]

            for (j in (i + 1 until bodies.size)) {
                val b = bodies[j]

                if (a.isStatic && b.isStatic)
                    continue

                if (a.bounds intersects b.bounds) {
                    val pair = CollisionPair(a, b)
                    pair.shouldCollide = collisionFilter?.shouldCollide(a.fixture, b.fixture) ?: true
                    collisionPairs += pair
                }
            }
        }

        collisionPairs.forEach {
            val manifold = getCollisionManifold(it.a, it.b)
            if (manifold != null) {
                it.manifold = manifold
                collisionListener?.onCollision(it)
                if (it.shouldCollide)
                    manifolds += manifold
            }
        }

        bodies.forEach {
            integrateForces(it)
        }

        manifolds.forEach {
            initializeManifold(it)
        }

        repeat(resolveIterations) {
            manifolds.forEach {
                resolveCollision(it)
            }
        }

        bodies.forEach {
            integrateVelocity(it)
        }

        if (doPositionalCorrection)
            manifolds.forEach {
                positionalCorrection(it)
            }

        if (doPositionRounding) {
            bodies.forEach {
                if (it.positionX % 1.0f <= positionRoundingThreshold || 1.0f - (it.positionX % 1.0f) <= positionRoundingThreshold)
                    it.positionX = round(it.positionX)

                if (it.positionY % 1.0f <= positionRoundingThreshold || 1.0f - (it.positionY % 1.0f) <= positionRoundingThreshold)
                    it.positionY = round(it.positionY)
            }
        }

        bodies.forEach {
            it.force.setZero()
            it.torque = 0.0f
            it.update()
        }
    }

    fun getCollisionManifold(a: Body, b: Body): Manifold? {
        var flip = false

        val sa = a.fixture.shape
        val sb = b.fixture.shape

        val manifold = when (sa) {
            is AxisAlignedRectangleShape -> when (sb) {
                is AxisAlignedRectangleShape -> Collision.collideAxisAlignedRectangleToAxisAlignedRectangle(sa, sb)
                is CircleShape -> {
                    flip = true
                    Collision.collideCircleToAxisAlignedRectangle(sb, sa)
                }
                is PolygonShape -> Collision.collideAxisAlignedRectangleToPolygon(sa, sb)
            }
            is CircleShape -> when (sb) {
                is CircleShape -> Collision.collideCircleToCircle(sa, sb)
                is AxisAlignedRectangleShape -> Collision.collideCircleToAxisAlignedRectangle(sa, sb)
                is PolygonShape -> Collision.collideCircleToPolygon(sa, sb)
            }
            is PolygonShape -> when (sb) {
                is CircleShape -> {
                    flip = true
                    Collision.collideCircleToPolygon(sb, sa)
                }
                is AxisAlignedRectangleShape -> {
                    flip = true
                    Collision.collideAxisAlignedRectangleToPolygon(sb, sa)
                }
                is PolygonShape -> Collision.collidePolygonToPolygon(sa, sb)
            }
        }

        if (manifold != null) {
            if (flip) {
                manifold.normal.x *= -1.0f
                manifold.normal.y *= -1.0f
                manifold.a = b
                manifold.b = a
            } else {
                manifold.a = a
                manifold.b = b
            }
        }

        return manifold
    }

    fun addBody(body: Body) {
        bodies += body
    }

    fun removeBody(body: Body) {
        bodies -= body
    }

    fun clear() {
        bodies.clear()
    }

    fun getAllOverlappingCircle(x: Float, y: Float, radius: Float, dest: MutableList<Body> = arrayListOf()): MutableList<Body> {
        forEachOverlappingCircle(x, y, radius) {
            dest += it
        }
        return dest
    }

    fun forEachOverlappingCircle(x: Float, y: Float, radius: Float, block: (Body) -> Unit) {
        bodies.forEach {
            if (it.fixture.overlapsCircle(x, y, radius))
                block(it)
        }
    }

    fun getAllOverlappingRectangle(x: Float, y: Float, width: Float, height: Float, dest: MutableList<Body> = arrayListOf()): MutableList<Body> {
        forEachOverlappingRectangle(x, y, width, height) {
            dest += it
        }
        return dest
    }

    fun forEachOverlappingRectangle(x: Float, y: Float, width: Float, height: Float, block: (Body) -> Unit) {
        bodies.forEach {
            if (it.fixture.overlapsRectangle(x, y, width, height))
                block(it)
        }
    }

    fun raycast(ray: Ray): RaycastResult? {
        var result: RaycastResult? = null
        bodies.forEach {
            it.fixture.raycast(ray)?.let {
                if (it.distance < (result?.distance ?: Float.MAX_VALUE))
                    result = it
            }
        }
        return result
    }
}
