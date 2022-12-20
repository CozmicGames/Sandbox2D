package engine.graphics.ui.style

import com.cozmicgames.utils.Color
import com.cozmicgames.utils.Disposable
import kotlin.reflect.KProperty

class GUIStyle : Disposable {
    enum class Type {
        COLOR,
        STRING,
        BOOLEAN,
        INT,
        FLOAT
    }

    private val values = hashMapOf<String, Any>()

    operator fun <T : Any> set(name: String, value: T) {
        (values.put(name, value) as? Disposable?)?.dispose()
    }

    operator fun <T : Any> get(name: String, default: () -> T) = values.getOrPut(name, default)

    override fun dispose() {
        for ((_, value) in values)
            if (value is Disposable)
                value.dispose()
    }
}

class GUIStyleAccessor<T : Any>(private val style: GUIStyle, private val supplier: () -> T) {
    operator fun getValue(thisRef: Any, property: KProperty<*>): T = style[property.name, supplier] as T

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        style[property.name] = value
    }
}

fun GUIStyle.color(supplier: () -> Color) = GUIStyleAccessor(this, supplier)

fun GUIStyle.string(supplier: () -> String) = GUIStyleAccessor(this, supplier)

fun GUIStyle.boolean(supplier: () -> Boolean) = GUIStyleAccessor(this, supplier)

fun GUIStyle.int(supplier: () -> Int) = GUIStyleAccessor(this, supplier)

fun GUIStyle.float(supplier: () -> Float) = GUIStyleAccessor(this, supplier)
