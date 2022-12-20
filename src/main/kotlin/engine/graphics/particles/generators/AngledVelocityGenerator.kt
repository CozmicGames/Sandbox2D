package engine.graphics.particles.generators

import com.cozmicgames.utils.maths.lerp
import com.cozmicgames.utils.maths.randomFloat
import com.cozmicgames.utils.maths.toRadians
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleGenerator
import engine.graphics.particles.data.VelocityData
import kotlin.math.cos
import kotlin.math.sin

open class AngledVelocityGenerator(var minAngle: Float, var maxAngle: Float, var minStartSpeed: Float, var maxStartSpeed: Float) : ParticleGenerator {
    override fun generate(data: ParticleData, start: Int, end: Int) {
        val velocities = data.getArray { VelocityData() }

        repeat(end - start) {
            val phi = toRadians(lerp(minAngle, maxAngle, randomFloat()) - 90.0f)
            val speed = lerp(minStartSpeed, maxStartSpeed, randomFloat())

            velocities[it + start].x = cos(phi) * speed
            velocities[it + start].y = sin(phi) * speed
        }
    }
}