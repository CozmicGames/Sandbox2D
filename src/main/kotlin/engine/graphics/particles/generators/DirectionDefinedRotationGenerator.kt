package engine.graphics.particles.generators

import com.cozmicgames.utils.maths.HALF_PI
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleGenerator
import engine.graphics.particles.data.AngleData
import engine.graphics.particles.data.VelocityData
import kotlin.math.atan2

class DirectionDefinedRotationGenerator(var minStartAngle: Float, val maxStartAngle: Float, var minEndAngle: Float, val maxEndAngle: Float) : ParticleGenerator {
    override fun generate(data: ParticleData, start: Int, end: Int) {
        val angles = data.getArray { AngleData() }
        val velocities = data.getArray { VelocityData() }
        repeat(end - start) {
            with(angles[it + start]) {
                val phi = HALF_PI - atan2(-velocities[it].y, velocities[it].x)
                startAngle = phi
                endAngle = phi
                angle = phi
            }
        }
    }
}