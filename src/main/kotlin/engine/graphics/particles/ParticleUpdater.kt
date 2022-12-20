package engine.graphics.particles

interface ParticleUpdater {
    fun init(data: ParticleData)
    fun update(data: ParticleData, delta: Float)
}