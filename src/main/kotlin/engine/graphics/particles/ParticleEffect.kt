package engine.graphics.particles

import com.cozmicgames.utils.Color
import com.cozmicgames.utils.maths.Matrix3x2
import com.cozmicgames.utils.maths.toRadians
import engine.Game
import engine.graphics.asRegion
import engine.graphics.drawRect
import engine.graphics.particles.data.*
import engine.graphics.shaders.ParticleShader
import engine.assets.managers.shaders
import kotlin.math.floor
import kotlin.math.min

class ParticleEffect(maxParticles: Int, var emitRate: Float) {
    companion object {
        init {
            Game.assets.shaders?.add("particle", ParticleShader)
        }
    }

    private val generators = arrayListOf<ParticleGenerator>()
    private val spawners = arrayListOf<ParticleSpawner>()
    private val updaters = arrayListOf<ParticleUpdater>()

    private val data = ParticleData(maxParticles)
    private var time = 0.0f

    fun addGenerator(generator: ParticleGenerator) {
        generators += generator
    }

    fun removeGenerator(generator: ParticleGenerator) {
        generators -= generator
    }

    fun addSpawner(spawner: ParticleSpawner) {
        spawners += spawner
    }

    fun removeSpawner(spawner: ParticleSpawner) {
        spawners -= spawner
    }

    fun addUpdater(updater: ParticleUpdater) {
        updaters += updater
        updater.init(data)
    }

    fun removeUpdater(updater: ParticleUpdater) {
        updaters -= updater
    }

    fun clearGenerators() = generators.clear()

    fun clearSpawners() = spawners.clear()

    fun clearUpdaters() = updaters.clear()

    fun reset() {
        data.numberOfAlive = 0
    }

    fun emit(count: Int) {
        if (spawners.isEmpty())
            return

        val start = data.numberOfAlive
        val end = min(start + count, data.maxParticles - 1)
        val numberOfNewParticles = end - start

        val spawnerCount = numberOfNewParticles / spawners.size
        val remainder = numberOfNewParticles - spawnerCount * spawners.size

        var spawnerStart = start

        spawners.forEachIndexed { index, spawner ->
            val numberToSpawn = if (index < remainder) spawnerCount + 1 else spawnerCount
            spawners[index].spawn(data, spawnerStart, spawnerStart + numberToSpawn)
            spawnerStart += numberToSpawn
        }

        generators.forEach {
            it.generate(data, start, end)
        }

        data.numberOfAlive += numberOfNewParticles
    }

    fun update(delta: Float) {
        if (emitRate > 0.0f) {
            time += delta

            if (time * emitRate > 1.0f) {
                val count = floor(time * emitRate).toInt()
                time -= (count / emitRate)

                emit(count)
            }
        }

        updaters.forEach {
            it.update(data, delta)
        }
    }

    fun render(layer: Int, transform: Matrix3x2? = null) {
        val positions = data.getArrayOrNull<PositionData>() ?: return
        val sizes = data.getArrayOrNull<SizeData>() ?: return

        val angles = data.getArrayOrNull<AngleData>()
        val textures = data.getArrayOrNull<TextureData>()
        val colors = data.getArrayOrNull<ColorData>()

        var index = 0

        while (index < data.numberOfAlive) {
            var currentTexture = textures?.get(index)?.region

            Game.renderer.submit(layer, currentTexture?.texture ?: Game.graphics2d.blankTexture, "particle", false, false) { context ->
                transform?.let {
                    context.pushMatrix(it)
                }

                while (index < data.numberOfAlive) {
                    if (textures?.get(index)?.region != currentTexture) {
                        currentTexture = textures?.get(index)?.region
                        break
                    }

                    val angle = angles?.get(index)?.angle ?: 0.0f
                    val color = colors?.get(index)?.color ?: Color.WHITE

                    val position = positions[index]
                    val size = sizes[index]

                    index++

                    val region = currentTexture ?: Game.graphics2d.blankTexture.asRegion()

                    context.drawRect(position.x, position.y, size.size, size.size, color, toRadians(angle), region.u0, region.v0, region.u1, region.v1)
                }

                transform?.let {
                    context.popMatrix()
                }
            }
        }
    }
}