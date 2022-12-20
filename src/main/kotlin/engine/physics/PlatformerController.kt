package engine.physics

import com.cozmicgames.Kore
import com.cozmicgames.utils.events.EventContext
import com.cozmicgames.utils.maths.Vector2
import com.cozmicgames.utils.maths.smoothDamp
import engine.Game

class PlatformerController(var body: Body) {
    class LandedEvent(val body: Body)

    var jumpForce = 100.0f
    var crouchSpeedFactor = 0.5f
    var movementSpeed = 100.0f
    var movementSmoothing = 0.05f
    var doAllowAirControl = false
    var airControlSpeedFactor = 0.5f
    var maxJumpCount = 1
    var gravityFactor = 1.05f

    var isOnGround = false
        private set

    private val velocity = Vector2()
    private var jumpCount = 0

    fun update() {
        if (isOnGround)
            jumpCount = 0

        val wasOnGround = isOnGround
        isOnGround = false

        val groundCheckX = body.bounds.centerX
        val groundCheckY = body.bounds.minY
        val groundCheckRadius = 0.001f

        if (body.velocity.y <= 0.0f)
            Game.physics.forEachOverlappingCircle(groundCheckX, groundCheckY, groundCheckRadius) {
                if (it != body)
                    isOnGround = true
            }

        if (isOnGround && !wasOnGround)
            Kore.context.inject<EventContext>()?.dispatch(LandedEvent(body))
    }

    fun move(amount: Float, crouch: Boolean, jump: Boolean, delta: Float) {
        var move = amount * movementSpeed

        if (isOnGround || doAllowAirControl) {
            if (crouch)
                move *= crouchSpeedFactor

            if (!isOnGround)
                move *= airControlSpeedFactor

            val targetVelocity = Vector2(move, body.velocity.y)
            body.velocity.set(smoothDamp(body.velocity, targetVelocity, velocity, movementSmoothing, Float.MAX_VALUE, delta))
        }

        if (!isOnGround && body.velocity.y < 0.0f)
            body.velocity.y *= gravityFactor

        if (jumpCount < maxJumpCount && jump) {
            isOnGround = false
            //body.applyForce(0.0f, jumpForce * -Game.physics.gravity.y)
            body.velocity.y += jumpForce

            jumpCount++
        }
    }
}