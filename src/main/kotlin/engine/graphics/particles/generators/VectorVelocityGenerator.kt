package engine.graphics.particles.generators

import com.cozmicgames.utils.maths.Vector2
import com.cozmicgames.utils.maths.lerp
import com.cozmicgames.utils.maths.randomFloat
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleGenerator
import engine.graphics.particles.data.VelocityData

class VectorVelocityGenerator(var minVelocity: Vector2, val maxVelocity: Vector2) : ParticleGenerator {
    override fun generate(data: ParticleData, start: Int, end: Int) {
        val velocities = data.getArray { VelocityData() }

        repeat(end - start) {
            val r = randomFloat()
            velocities[it + start].x = lerp(minVelocity.x, maxVelocity.x, r)
            velocities[it + start].y = lerp(minVelocity.y, maxVelocity.y, r)
        }
    }
}