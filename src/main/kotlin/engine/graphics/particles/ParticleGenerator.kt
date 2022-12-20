package engine.graphics.particles

interface ParticleGenerator {
    fun generate(data: ParticleData, start: Int, end: Int)
}