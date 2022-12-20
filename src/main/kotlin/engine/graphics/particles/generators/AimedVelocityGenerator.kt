package engine.graphics.particles.generators

import com.cozmicgames.utils.maths.Vector2
import com.cozmicgames.utils.maths.lerp
import com.cozmicgames.utils.maths.normalized
import com.cozmicgames.utils.maths.randomFloat
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleGenerator
import engine.graphics.particles.data.PositionData
import engine.graphics.particles.data.VelocityData

class AimedVelocityGenerator(var target: Vector2, var minStartSpeed: Float, var maxStartSpeed: Float) : ParticleGenerator {
    override fun generate(data: ParticleData, start: Int, end: Int) {
        val velocities = data.getArray { VelocityData() }
        val positions = data.getArray { PositionData() }
        repeat(end - start) {
            val dx = target.x - positions[it + start].x
            val dy = target.y - positions[it + start].y
            normalized(dx, dy) { ndx, ndy ->
                val speed = lerp(minStartSpeed, maxStartSpeed, randomFloat())

                velocities[it + start].x = ndx * speed
                velocities[it + start].y = ndy * speed
            }
        }
    }
}