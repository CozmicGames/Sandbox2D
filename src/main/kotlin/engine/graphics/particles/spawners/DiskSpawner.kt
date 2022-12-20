package engine.graphics.particles.spawners

import com.cozmicgames.utils.maths.TWO_PI
import com.cozmicgames.utils.maths.Vector2
import com.cozmicgames.utils.maths.randomFloat
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleSpawner
import engine.graphics.particles.data.PositionData
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class DiskSpawner(val center: Vector2, var radius: Float) : ParticleSpawner {
    override fun spawn(data: ParticleData, start: Int, end: Int) {
        val positions = data.getArray { PositionData() }

        repeat(end - start) {
            val phi = randomFloat() * TWO_PI
            val a = sqrt(randomFloat())
            positions[it + start].x = center.x + a * radius * cos(phi)
            positions[it + start].y = center.y + a * radius * sin(phi)
        }
    }
}