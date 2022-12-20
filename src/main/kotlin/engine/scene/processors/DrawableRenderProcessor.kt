package engine.scene.processors

import engine.Game
import engine.scene.SceneProcessor
import engine.scene.components.*

class DrawableRenderProcessor : SceneProcessor() {
    override fun shouldProcess(delta: Float) = true

    override fun process(delta: Float) {
        val scene = scene ?: return

        for (gameObject in scene.activeGameObjects) {
            val drawableProviderComponent = gameObject.getComponent<DrawableProviderComponent>() ?: continue

            drawableProviderComponent.drawables.forEach {
                Game.renderer.submit(it, drawableProviderComponent.isFlippedX, drawableProviderComponent.isFlippedY)
            }
        }
    }
}