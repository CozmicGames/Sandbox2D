package engine.scene.processors

import engine.Game
import engine.scene.SceneProcessor
import engine.scene.components.*

class SpriteRenderProcessor : SceneProcessor() {
    override fun shouldProcess(delta: Float) = true

    override fun process(delta: Float) {
        val scene = scene ?: return

        for (gameObject in scene.activeGameObjects) {
            val spriteComponent = gameObject.getComponent<SpriteComponent>() ?: continue

            Game.renderer.submit(spriteComponent, spriteComponent.isFlippedX, spriteComponent.isFlippedY)
        }
    }
}