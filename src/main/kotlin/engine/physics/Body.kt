package engine.physics

import com.cozmicgames.utils.maths.FLOAT_EPSILON
import com.cozmicgames.utils.maths.Rectangle
import com.cozmicgames.utils.maths.Vector2
import com.cozmicgames.utils.maths.cross
import engine.utils.Transform
import kotlin.math.abs

open class Body(val transform: Transform = Transform()) {
    val bounds = Rectangle()

    lateinit var fixture: Fixture<*>
        private set

    var positionX by transform::x
    var positionY by transform::y
    var rotation by transform::rotation
    var scaleX by transform::scaleX
    var scaleY by transform::scaleY

    val force = Vector2()
    var torque = 0.0f

    val velocity = Vector2()
    var angularVelocity = 0.0f

    var mass = 0.0f
    var inertia = 0.0f

    val inverseMass get() = if (mass == 0.0f) 0.0f else 1.0f / mass
    val inverseInertia get() = if (inertia == 0.0f) 0.0f else 1.0f / inertia

    var restitution = 0.2f
    var staticFriction = 0.5f
    var dynamicFriction = 0.3f
    var gravityScale = 1.0f

    val isStatic get() = inverseMass == 0.0f

    val isRotationLocked get() = inverseInertia == 0.0f

    val isResting get() = abs(velocity.x) <= FLOAT_EPSILON && abs(velocity.y) <= FLOAT_EPSILON

    fun applyImpulse(x: Float, y: Float, contactX: Float, contactY: Float) {
        velocity.x += inverseMass * x
        velocity.y += inverseMass * y

        angularVelocity += inverseInertia * cross(contactX, contactY, x, y)
    }

    fun applyForce(x: Float, y: Float) {
        force.x += x
        force.y += y
    }

    fun applyTorque(torque: Float) {
        this.torque += torque
    }

    fun setStatic() {
        mass = 0.0f
        inertia = 0.0f
    }

    fun lockRotation() {
        inertia = 0.0f
    }

    fun calculateMassAndInertia() {
        mass = 0.0f
        inertia = 0.0f
        fixture.calculateMassAndInertia()
    }

    fun setShape(shape: Shape, density: Float = 0.0f) {
        fixture = when (shape) {
            is CircleShape -> CircleFixture(shape, density)
            is AxisAlignedRectangleShape -> AxisAlignedRectangleFixture(shape, density)
            is PolygonShape -> PolygonFixture(shape, density)
        }
        fixture.body = this
        fixture.update()
    }

    fun update() {
        bounds.infinite()
        fixture.update()
    }
}