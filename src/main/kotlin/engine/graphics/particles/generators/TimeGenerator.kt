package engine.graphics.particles.generators

import com.cozmicgames.utils.maths.lerp
import com.cozmicgames.utils.maths.randomFloat
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleGenerator
import engine.graphics.particles.data.TimeData

class TimeGenerator(var minLifeTime: Float, var maxLifeTime: Float) : ParticleGenerator {
    override fun generate(data: ParticleData, start: Int, end: Int) {
        val times = data.getArray { TimeData() }

        repeat(end - start) {
            with(times[it + start]) {
                lifeTime = lerp(minLifeTime, maxLifeTime, randomFloat())
                remainingLifeTime = lifeTime
                interpolationValue = 0.0f
            }
        }
    }
}