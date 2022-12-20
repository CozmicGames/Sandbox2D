package engine.scene.components

import com.cozmicgames.Kore
import com.cozmicgames.log
import com.cozmicgames.utils.Properties
import engine.scene.Component
import engine.scene.GameObject
import engine.scene.Scene

class NameComponent : Component() {
    var name = ""
        set(value) {
            if (gameObject.scene.findGameObjectByName(value) != null) {
                Kore.log.error(this::class, "The name $value already exists. Names must be unique across the same scene!")
                return
            }

            field = value
        }

    override fun read(properties: Properties) {
        properties.getString("name")?.let { name = it }
    }

    override fun write(properties: Properties) {
        properties.setString("name", name)
    }
}

fun Scene.findGameObjectByName(name: String): GameObject? {
    gameObjects.forEach {
        val nameComponent = it.getComponent<NameComponent>()
        if (nameComponent?.name == name)
            return it
    }

    return null
}
