package engine.graphics.particles.updaters

import com.cozmicgames.utils.maths.lerp
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleUpdater
import engine.graphics.particles.data.AngleData
import engine.graphics.particles.data.TimeData

class RotationUpdater : ParticleUpdater {
    private lateinit var angles: Array<AngleData>
    private lateinit var times: Array<TimeData>

    override fun init(data: ParticleData) {
        angles = data.getArray { AngleData() }
        times = data.getArray { TimeData() }
    }

    override fun update(data: ParticleData, delta: Float) {
        repeat(data.numberOfAlive) {
            angles[it].angle = lerp(angles[it].startAngle, angles[it].endAngle, times[it].interpolationValue)
        }
    }
}