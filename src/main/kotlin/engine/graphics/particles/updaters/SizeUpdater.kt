package engine.graphics.particles.updaters

import com.cozmicgames.utils.maths.lerp
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleUpdater
import engine.graphics.particles.data.SizeData
import engine.graphics.particles.data.TimeData

class SizeUpdater : ParticleUpdater {
    private lateinit var sizes: Array<SizeData>
    private lateinit var times: Array<TimeData>

    override fun init(data: ParticleData) {
        sizes = data.getArray { SizeData() }
        times = data.getArray { TimeData() }
    }

    override fun update(data: ParticleData, delta: Float) {
        repeat(data.numberOfAlive) {
            sizes[it].size = lerp(sizes[it].startSize, sizes[it].endSize, times[it].interpolationValue)
        }
    }
}