package engine.graphics.particles.generators

import com.cozmicgames.utils.Color
import com.cozmicgames.utils.maths.lerp
import com.cozmicgames.utils.maths.randomFloat
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleGenerator
import engine.graphics.particles.data.ColorData

class ColorGenerator(var minStartColor: Color, var maxStartColor: Color, var minEndColor: Color, var maxEndColor: Color) : ParticleGenerator {
    override fun generate(data: ParticleData, start: Int, end: Int) {
        val colors = data.getArray { ColorData() }

        repeat(end - start) {
            with(colors[it + start]) {
                val rs = randomFloat()
                startColor.r = lerp(minStartColor.r, maxStartColor.r, rs)
                startColor.g = lerp(minStartColor.g, maxStartColor.g, rs)
                startColor.b = lerp(minStartColor.b, maxStartColor.b, rs)
                startColor.a = lerp(minStartColor.a, maxStartColor.a, rs)

                val re = randomFloat()
                endColor.r = lerp(minEndColor.r, maxEndColor.r, rs)
                endColor.g = lerp(minEndColor.g, maxEndColor.g, rs)
                endColor.b = lerp(minEndColor.b, maxEndColor.b, rs)
                endColor.a = lerp(minEndColor.a, maxEndColor.a, rs)

                color.set(startColor)
            }
        }
    }
}