package engine.graphics.particles.generators

import com.cozmicgames.utils.maths.randomFloat
import com.cozmicgames.utils.maths.toRadians
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleGenerator
import engine.graphics.particles.data.AngleData

class RotationGenerator(var minStartAngle: Float, val maxStartAngle: Float, var minEndAngle: Float, val maxEndAngle: Float) : ParticleGenerator {
    override fun generate(data: ParticleData, start: Int, end: Int) {
        val angles = data.getArray { AngleData() }

        repeat(end - start) {
            with(angles[it + start]) {
                startAngle = toRadians(minStartAngle + randomFloat() * (maxStartAngle - minStartAngle))
                endAngle = toRadians(minEndAngle + randomFloat() * (maxEndAngle - minEndAngle))
                angle = startAngle
            }
        }
    }
}