package engine.graphics.particles.data

import engine.graphics.particles.ParticleData

data class TimeData(var remainingLifeTime: Float=0.0f, var lifeTime: Float=0.0f, var interpolationValue: Float = 0.0f) : ParticleData.DataType

