package engine.graphics.particles.generators

import com.cozmicgames.utils.maths.randomFloat
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleGenerator
import engine.graphics.particles.data.SizeData

class SizeGenerator(var minStartSize: Float, val maxStartSize: Float, var minEndSize: Float, val maxEndSize: Float) : ParticleGenerator {
    override fun generate(data: ParticleData, start: Int, end: Int) {
        val sizes = data.getArray { SizeData() }

        repeat(end - start) {
            with(sizes[it + start]) {
                startSize = minStartSize + randomFloat() * (maxStartSize - minStartSize)
                endSize = minEndSize + randomFloat() * (maxEndSize - minEndSize)
                size = startSize
            }
        }
    }
}