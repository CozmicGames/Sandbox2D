package engine.graphics.particles.spawners

import com.cozmicgames.utils.maths.TWO_PI
import com.cozmicgames.utils.maths.Vector2
import com.cozmicgames.utils.maths.randomFloat
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleSpawner
import engine.graphics.particles.data.PositionData
import kotlin.math.cos
import kotlin.math.sin

class CircleSpawner(val center: Vector2, var radius: Float) : ParticleSpawner {
    override fun spawn(data: ParticleData, start: Int, end: Int) {
        val positions = data.getArray { PositionData() }

        repeat(end - start) {
            val phi = randomFloat() * TWO_PI

            positions[it + start].x = center.x + radius * cos(phi)
            positions[it + start].y = center.y + radius * sin(phi)
        }
    }
}