package engine.graphics.particles.updaters

import com.cozmicgames.utils.maths.Vector2
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleUpdater
import engine.graphics.particles.data.AccelerationData
import engine.graphics.particles.data.PositionData
import engine.graphics.particles.data.VelocityData

class MovementUpdater(val globalAcceleration: Vector2 = Vector2.ZERO) : ParticleUpdater {
    private lateinit var positions: Array<PositionData>
    private lateinit var accelerations: Array<AccelerationData>
    private lateinit var velocities: Array<VelocityData>

    override fun init(data: ParticleData) {
        positions = data.getArray { PositionData() }
        accelerations = data.getArray { AccelerationData() }
        velocities = data.getArray { VelocityData() }
    }

    override fun update(data: ParticleData, delta: Float) {
        repeat(data.numberOfAlive) {
            accelerations[it].x += globalAcceleration.x
            accelerations[it].y += globalAcceleration.y
            positions[it].x += delta * velocities[it].x
            positions[it].y += delta * velocities[it].y
            velocities[it].x += delta * accelerations[it].x
            velocities[it].y += delta * accelerations[it].y
            accelerations[it].x = 0.0f
            accelerations[it].y = 0.0f
        }
    }
}