package engine.scene.components

import com.cozmicgames.utils.Properties
import engine.scene.Component
import engine.utils.Transform

class TransformComponent : Component() {
    val transform = Transform()

    override fun onAdded() = updateParent()

    override fun onParentChanged() = updateParent()

    private fun updateParent() {
        val parentTransform = gameObject.parent?.getComponent<TransformComponent>()?.transform
        transform.parent = parentTransform
    }

    override fun read(properties: Properties) {
        transform.read(properties)
    }

    override fun write(properties: Properties) {
        transform.write(properties)
    }
}
