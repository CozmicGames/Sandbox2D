package engine.graphics.particles.updaters

import com.cozmicgames.utils.maths.dot
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleUpdater
import engine.graphics.particles.data.AccelerationData
import engine.graphics.particles.data.PositionData

class AttractorUpdater(vararg attractors: Attractor) : ParticleUpdater {
    data class Attractor(var x: Float, var y: Float, var force: Float)

    private val attractors = arrayListOf<Attractor>()

    private lateinit var positions: Array<PositionData>
    private lateinit var accelerations: Array<AccelerationData>


    init {
        attractors.forEach {
            addAttractor(it)
        }
    }

    fun addAttractor(x: Float, y: Float, force: Float) = Attractor(x, y, force).also { addAttractor(it) }

    fun addAttractor(attractor: Attractor) {
        attractors += attractor
    }

    fun removeAttractor(attractor: Attractor) {
        attractors -= attractor
    }

    override fun init(data: ParticleData) {
        positions = data.getArray { PositionData() }
        accelerations = data.getArray { AccelerationData() }
    }

    override fun update(data: ParticleData, delta: Float) {
        repeat(data.numberOfAlive) {
            attractors.forEach { attractor ->
                val offsetX = attractor.x - positions[it].x
                val offsetY = attractor.y - positions[it].y

                val dist = attractor.force / dot(offsetX, offsetY, offsetX, offsetY)
                accelerations[it].x += offsetX * dist
                accelerations[it].y += offsetY * dist
            }
        }
    }
}