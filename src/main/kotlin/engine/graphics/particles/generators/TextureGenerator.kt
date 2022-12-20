package engine.graphics.particles.generators

import engine.graphics.TextureRegion
import engine.graphics.particles.ParticleData
import engine.graphics.particles.ParticleGenerator
import engine.graphics.particles.data.TextureData

class TextureGenerator(var region: TextureRegion) : ParticleGenerator {
    override fun generate(data: ParticleData, start: Int, end: Int) {
        val textures = data.getArray { TextureData() }

        repeat(end - start) {
            textures[it + start].region = region
        }
    }
}