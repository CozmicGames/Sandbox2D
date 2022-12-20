package engine.scene.processors

import engine.physics.Physics
import engine.scene.SceneProcessor

class PhysicsProcessor : SceneProcessor() {
    val physics = Physics()

    override fun shouldProcess(delta: Float) = true

    override fun process(delta: Float) {
        physics.update(delta)
    }
}