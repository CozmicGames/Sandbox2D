package engine.scene.components

import com.cozmicgames.utils.Properties
import engine.scene.Component
import engine.scene.GameObject
import engine.scene.Scene

class TagComponent : Component() {
    val tags = arrayListOf<String>()

    override fun read(properties: Properties) {
        tags.clear()
        properties.getStringArray("tags")?.let {
            tags.addAll(it)
        }
    }

    override fun write(properties: Properties) {
        properties.setStringArray("tags", tags.toTypedArray())
    }
}

fun Scene.findGameObjectsByTag(vararg tags: String, block: (GameObject) -> Unit) {
    outer@ for (gameObject in gameObjects) {
        val tagComponent = gameObject.getComponent<TagComponent>() ?: continue

        for (tag in tags) {
            if (tag !in tagComponent.tags)
                continue@outer
        }

        block(gameObject)
    }
}
