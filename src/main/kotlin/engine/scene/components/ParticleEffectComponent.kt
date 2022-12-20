package engine.scene.components

import com.cozmicgames.utils.Updateable
import engine.graphics.particles.ParticleEffect
import engine.scene.Component

class ParticleEffectComponent : Component(), Updateable {
    var create: ParticleEffect.() -> Unit = {}
        set(value) {
            isDirty = true
            field = value
        }

    var maxParticles = 100
        set(value) {
            isDirty = true
            field = value
        }

    var emitRate = 10.0f
        set(value) {
            isDirty = true
            field = value
        }

    var layer = 0

    lateinit var effect: ParticleEffect
        private set

    private var isDirty = true

    override fun update(delta: Float) {
        if (isDirty) {
            effect = ParticleEffect(maxParticles, emitRate)
            create(effect)
            isDirty = false
        }
        effect.update(delta)
    }
}