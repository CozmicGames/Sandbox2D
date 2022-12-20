package engine.scene

import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.Properties
import com.cozmicgames.utils.UUID
import com.cozmicgames.utils.Updateable
import kotlin.reflect.KClass

class Scene : Disposable {
    val activeGameObjects = object : Iterable<GameObject> {
        override fun iterator() = object : Iterator<GameObject> {
            private val gameObjects = gameObjectsInternal
            private var index = 0
            private var next: GameObject? = findNext()

            private fun findNext(): GameObject? {
                while (index < gameObjects.size) {
                    val gameObject = gameObjects[index]
                    index++

                    if (gameObject.isActive)
                        return gameObject
                }

                return null
            }

            override fun hasNext() = next != null

            override fun next(): GameObject {
                val value = requireNotNull(next)
                next = findNext()
                return value
            }
        }
    }

    val gameObjects = object : Iterable<GameObject> {
        override fun iterator() = gameObjectsInternal.iterator()
    }

    private val gameObjectsInternal = arrayListOf<GameObject>()
    private val processors = arrayListOf<SceneProcessor>()

    private val onAddedComponents = arrayListOf<Component>()
    private val onRemovedComponents = arrayListOf<Component>()
    private val onParentChangedComponents = arrayListOf<Component>()
    private val onActiveChangedComponents = arrayListOf<Component>()
    private val workingComponents = arrayListOf<Component>()

    fun onAddComponent(component: Component) {
        onAddedComponents += component
    }

    fun onRemoveComponent(component: Component) {
        onRemovedComponents += component
    }

    fun onParentChanged(gameObject: GameObject) {
        gameObject.forEach {
            onParentChangedComponents += it
        }
    }

    fun onActiveChanged(gameObject: GameObject) {
        gameObject.forEach {
            onActiveChangedComponents += it
        }
    }

    fun addGameObject(uuid: UUID = UUID.randomUUID(), block: GameObject.() -> Unit = {}): GameObject {
        val gameObject = GameObject(this, uuid)
        block(gameObject)
        gameObjectsInternal.add(gameObject)
        return gameObject
    }

    fun removeGameObject(gameObject: GameObject) {
        if (gameObjectsInternal.remove(gameObject)) {
            gameObject.children.forEach {
                removeGameObject(it)
            }
            gameObject.dispose()
        }
    }

    fun getGameObject(uuid: UUID): GameObject? {
        return gameObjectsInternal.find { it.uuid == uuid }
    }

    fun getOrAddGameObject(uuid: UUID): GameObject {
        val gameObject = getGameObject(uuid)

        if (gameObject != null)
            return gameObject

        return addGameObject(uuid)
    }

    fun addSceneProcessor(processor: SceneProcessor) {
        processors.add(processor)
        processor.scene = this
    }

    fun removeSceneProcessor(processor: SceneProcessor) {
        if (!processors.remove(processor))
            return

        processor.scene = null
    }

    inline fun <reified T : SceneProcessor> findSceneProcessor() = findSceneProcessor(T::class)

    fun <T : SceneProcessor> findSceneProcessor(type: KClass<T>): T? {
        return processors.find { type.isInstance(it) } as? T?
    }

    fun clearGameObjects() {
        gameObjectsInternal.forEach {
            it.dispose()
        }
    }

    fun clearProcessors() {
        processors.clear()
    }

    fun update(delta: Float) {
        workingComponents.clear()
        workingComponents.addAll(onAddedComponents)
        onAddedComponents.clear()
        workingComponents.forEach {
            it.onAdded()
        }

        workingComponents.clear()
        workingComponents.addAll(onParentChangedComponents)
        onParentChangedComponents.clear()
        workingComponents.forEach {
            it.onParentChanged()
        }

        workingComponents.clear()
        workingComponents.addAll(onActiveChangedComponents)
        onActiveChangedComponents.clear()
        workingComponents.forEach {
            it.onActiveChanged()
        }

        workingComponents.clear()
        workingComponents.addAll(onRemovedComponents)
        onRemovedComponents.clear()
        workingComponents.forEach {
            it.onRemoved()
        }

        activeGameObjects.forEach { gameObject ->
            gameObject.forEach {
                if (it is Updateable)
                    it.update(delta)
            }
        }

        processors.forEach {
            if (it.shouldProcess(delta))
                it.process(delta)
        }
    }

    fun read(properties: Properties) {
        val gameObjectsProperties = properties.getPropertiesArray("gameObjects") ?: return

        for (gameObjectProperties in gameObjectsProperties) {
            val uuidString = gameObjectProperties.getString("uuid") ?: continue

            val gameObject = getOrAddGameObject(UUID(uuidString))
            gameObject.read(gameObjectProperties)
        }
    }

    fun write(properties: Properties, filter: (GameObject) -> Boolean = { true }) {
        val gameObjectsProperties = arrayListOf<Properties>()

        gameObjectsInternal.forEach {
            if (filter(it)) {
                val gameObjectProperties = Properties()

                gameObjectProperties.setString("uuid", it.uuid.toString())
                it.write(gameObjectProperties)

                gameObjectsProperties += gameObjectProperties
            }
        }

        properties.setPropertiesArray("gameObjects", gameObjectsProperties.toTypedArray())
    }

    override fun dispose() {
        gameObjectsInternal.forEach {
            it.dispose()
        }
        clearGameObjects()

        processors.forEach {
            if (it is Disposable)
                it.dispose()
        }
        clearProcessors()
    }
}

inline fun <reified T : Component> Scene.findGameObjectsWithComponent(block: (GameObject) -> Unit) = findGameObjectsWithComponent(T::class, block)

inline fun <T : Component> Scene.findGameObjectsWithComponent(type: KClass<T>, block: (GameObject) -> Unit) {
    gameObjects.forEach {
        if (it.hasComponent(type))
            block(it)
    }
}

inline fun <reified T : Component> Scene.findGameObjectByComponent(block: (GameObject) -> Boolean) = findGameObjectByComponent(T::class, block)

inline fun <T : Component> Scene.findGameObjectByComponent(type: KClass<T>, block: (GameObject) -> Boolean): GameObject? {
    findGameObjectsWithComponent(type) {
        if (block(it))
            return it
    }

    return null
}
