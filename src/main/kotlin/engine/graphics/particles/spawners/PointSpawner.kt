package engine.graphics.particles.spawners

import com.cozmicgames.utils.maths.Vector2
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleSpawner
import engine.graphics.particles.data.PositionData

class PointSpawner(val point: Vector2) : ParticleSpawner {
    override fun spawn(data: ParticleData, start: Int, end: Int) {
        val positions = data.getArray { PositionData() }

        repeat(end - start) {
            positions[it + start].x = point.x
            positions[it + start].y = point.y
        }
    }
}