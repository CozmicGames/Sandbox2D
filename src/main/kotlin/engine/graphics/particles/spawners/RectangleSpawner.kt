package engine.graphics.particles.spawners

import com.cozmicgames.utils.maths.Rectangle
import com.cozmicgames.utils.maths.randomFloat
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleSpawner
import engine.graphics.particles.data.PositionData

class RectangleSpawner(val rectangle: Rectangle) : ParticleSpawner {
    override fun spawn(data: ParticleData, start: Int, end: Int) {
        val positions = data.getArray { PositionData() }

        repeat(end - start) {
            positions[it + start].x = rectangle.minX + randomFloat() * rectangle.width
            positions[it + start].y = rectangle.minY + randomFloat() * rectangle.height
        }
    }
}