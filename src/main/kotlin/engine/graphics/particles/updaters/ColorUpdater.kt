package engine.graphics.particles.updaters

import com.cozmicgames.utils.maths.lerp
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleUpdater
import engine.graphics.particles.data.ColorData
import engine.graphics.particles.data.TimeData

class ColorUpdater : ParticleUpdater {
    private lateinit var colors: Array<ColorData>
    private lateinit var times: Array<TimeData>

    override fun init(data: ParticleData) {
        colors = data.getArray { ColorData() }
        times = data.getArray { TimeData() }

    }

    override fun update(data: ParticleData, delta: Float) {
        repeat(data.numberOfAlive) {
            with(colors[it]) {
                color.r = lerp(startColor.r, endColor.r, times[it].interpolationValue)
                color.g = lerp(startColor.g, endColor.g, times[it].interpolationValue)
                color.b = lerp(startColor.b, endColor.b, times[it].interpolationValue)
                color.a = lerp(startColor.a, endColor.a, times[it].interpolationValue)
            }
        }
    }
}