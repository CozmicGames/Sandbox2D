package engine.scene

import com.cozmicgames.Kore
import com.cozmicgames.log
import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.Properties
import com.cozmicgames.utils.Reflection
import com.cozmicgames.utils.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class GameObject(val scene: Scene, val uuid: UUID) : Iterable<Component>, Disposable {
    inner class ComponentDelegate<T : Component>(private val type: KClass<T>, private val defaultInitializer: T.() -> Unit) {
        operator fun getValue(thisRef: Any, property: KProperty<*>): T {
            return if (!hasComponent(type))
                addComponent(type, defaultInitializer)
            else
                requireNotNull(getComponent(type))
        }
    }

    var parentUUID: UUID? = null
        set(value) {
            if (field == value)
                return

            parent = if (value == null)
                null
            else {
                val gameObject = scene.getOrAddGameObject(value)
                gameObject
            }

            field = value
        }
        get() = parent?.uuid

    var parent: GameObject? = null
        set(value) {
            if (field == value)
                return

            field?.childrenInternal?.remove(this)
            field = value
            value?.childrenInternal?.add(this)

            scene.onParentChanged(this)
        }

    private val childrenInternal = arrayListOf<GameObject>()

    val children get() = childrenInternal.asIterable()

    var isActive = true
        set(value) {
            if (field == value)
                return

            field = value
            childrenInternal.forEach {
                it.isActive = value
            }

            scene.onActiveChanged(this)
        }

    private val components = hashMapOf<KClass<*>, Component>()

    inline fun <reified T : Component> addComponent(noinline block: T.() -> Unit) = addComponent(T::class, block)

    fun <T : Component> addComponent(type: KClass<T>, initializer: T.() -> Unit): T {
        val component = Reflection.createInstance(type)
        if (component == null)
            Kore.log.fail(this::class, "Failed to create component of type $type")

        (components.put(type, requireNotNull(component)) as? Disposable?)?.dispose()
        component.gameObject = this

        initializer(component)
        scene.onAddComponent(component)

        return component
    }

    inline fun <reified T : Component> removeComponent() = removeComponent(T::class)

    fun <T : Component> removeComponent(type: KClass<T>) {
        val component = components.remove(type) ?: return

        scene.onRemoveComponent(component)

        if (component is Disposable)
            component.dispose()
    }

    inline fun <reified T : Component> hasComponent() = hasComponent(T::class)

    fun <T : Component> hasComponent(type: KClass<T>) = type in components

    inline fun <reified T : Component> getComponent() = getComponent(T::class)

    fun <T : Component> getComponent(type: KClass<T>) = components[type] as? T?

    inline fun <reified T : Component> getOrAddComponent(noinline defaultInitializer: T.() -> Unit = {}) = getOrAddComponent(T::class, defaultInitializer)

    fun <T : Component> getOrAddComponent(type: KClass<T>, defaultInitializer: T.() -> Unit = {}): T {
        return if (hasComponent(type))
            requireNotNull(getComponent(type))
        else
            addComponent(type, defaultInitializer)
    }

    override fun iterator(): Iterator<Component> = components.values.iterator()

    fun clearComponents() {
        components.values.forEach {
            it.onRemoved()
            if (it is Disposable)
                it.dispose()
        }
        components.clear()
    }

    fun read(properties: Properties) {
        clearComponents()

        properties.getString("parent")?.let { if (it != "null") parentUUID = UUID(it) }
        properties.getBoolean("active")?.let { isActive = it }

        properties.getPropertiesArray("components")?.let {
            for (componentProperties in it) {
                val componentTypeName = componentProperties.getString("type") ?: continue
                val componentType = Reflection.getClassByName(componentTypeName) ?: continue
                val component = Reflection.createInstance(componentType) as? Component ?: continue

                component.gameObject = this
                components[componentType] = component

                component.read(componentProperties)

                scene.onAddComponent(component)
            }
        }
    }

    fun write(properties: Properties) {
//        parentUUID?.let { properties.setString("parent", it.toString()) }
        properties.setString("parent", parentUUID?.toString() ?: "null")
        properties.setBoolean("active", isActive)

        val componentsProperties = arrayListOf<Properties>()

        forEach {
            val componentProperties = Properties()

            componentProperties.setString("type", Reflection.getClassName(it::class))
            it.write(componentProperties)

            componentsProperties += componentProperties
        }

        properties.setPropertiesArray("components", componentsProperties.toTypedArray())
    }

    override fun dispose() {
        clearComponents()
        parent = null
    }

    inline fun <reified T : Component> component(noinline defaultInitializer: T.() -> Unit = {}) = component(T::class, defaultInitializer)

    fun <T : Component> component(type: KClass<T>, defaultInitializer: T.() -> Unit = {}) = ComponentDelegate(type, defaultInitializer)
}

inline fun <reified T : Component> GameObject.findComponentInParentObjects() = findComponentInParentObjects(T::class)

fun <T : Component> GameObject.findComponentInParentObjects(type: KClass<T>): T? {
    parent?.let {
        val component = it.getComponent(type)

        if (component != null)
            return component

        return it.findComponentInParentObjects(type)
    }

    return null
}

fun GameObject.findGameObjectInChildren(filter: (GameObject) -> Boolean): GameObject? {
    children.forEach {
        if (filter(it))
            return it

        val result = it.findGameObjectInChildren(filter)

        if (result != null)
            return result
    }

    return null
}
