package engine.scene.processors

import engine.scene.SceneProcessor
import engine.scene.components.*

class ParticleRenderProcessor : SceneProcessor() {
    override fun shouldProcess(delta: Float) = true

    override fun process(delta: Float) {
        val scene = scene ?: return

        for (gameObject in scene.activeGameObjects) {
            val transformComponent = gameObject.getComponent<TransformComponent>() ?: continue
            val particleEffectComponent = gameObject.getComponent<ParticleEffectComponent>() ?: continue

            particleEffectComponent.effect.render(particleEffectComponent.layer, transformComponent.transform.global)
        }
    }
}