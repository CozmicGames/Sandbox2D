package engine.graphics.particles.updaters

import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleUpdater
import engine.graphics.particles.data.TimeData

class TimeUpdater : ParticleUpdater {
    private lateinit var times: Array<TimeData>

    override fun init(data: ParticleData) {
        times = data.getArray { TimeData() }
    }

    override fun update(data: ParticleData, delta: Float) {
        var count = data.numberOfAlive
        var i = 0

        while (i < count) {
            times[i].remainingLifeTime -= delta
            times[i].interpolationValue = 1.0f - (times[i].remainingLifeTime / times[i].lifeTime)

            if (times[i].remainingLifeTime <= 0.0f) {
                data.kill(i)
                count = data.numberOfAlive
            }

            i++
        }
    }
}