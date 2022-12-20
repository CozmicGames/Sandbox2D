package engine.graphics.particles

interface ParticleSpawner {
    fun spawn(data: ParticleData, start: Int, end: Int)
}