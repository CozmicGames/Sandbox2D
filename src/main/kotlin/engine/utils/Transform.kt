package engine.utils

import com.cozmicgames.utils.Properties
import com.cozmicgames.utils.maths.Matrix3x2
import com.cozmicgames.utils.maths.Vector2
import kotlin.math.cos
import kotlin.math.sin

class Transform(parent: Transform? = null) : Savable {
    private var isDirty = true
    private val children = arrayListOf<Transform>()

    private val internalLocal = Matrix3x2()
    private val internalGlobal = Matrix3x2()
    private val listeners = arrayListOf<() -> Unit>()

    val local: Matrix3x2
        get() {
            if (isDirty) {
                update()
                isDirty = false
            }
            return internalLocal
        }

    val global: Matrix3x2
        get() {
            if (isDirty) {
                update()
                isDirty = false
            }
            return internalGlobal
        }

    var x = 0.0f
        set(value) {
            field = value
            setDirty()
        }

    var y = 0.0f
        set(value) {
            field = value
            setDirty()
        }

    var rotation = 0.0f
        set(value) {
            field = value
            setDirty()
        }

    var scaleX = 1.0f
        set(value) {
            field = value
            setDirty()
        }

    var scaleY = 1.0f
        set(value) {
            field = value
            setDirty()
        }

    var parent: Transform? = parent
        set(value) {
            parent?.children?.remove(this)
            value?.children?.add(this)
            field = value
            setDirty()
        }

    private fun setDirty() {
        isDirty = true
        listeners.forEach {
            it()
        }
    }

    private fun update() {
        internalLocal.setToTranslationRotationScaling(x, y, rotation, scaleX, scaleY)

        val p = parent
        if (p == null)
            internalGlobal.set(internalLocal)
        else {
            val s = sin(p.rotation)
            val c = cos(p.rotation)
            val offsetX = x * c - y * s
            val offsetY = x * s + y * c

            internalGlobal.setToTranslationRotationScaling(p.x + offsetX, p.y + offsetY, p.rotation + rotation, scaleX, scaleY)
        }

        children.forEach {
            it.setDirty()
        }
    }

    fun addChangeListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeChangeListener(listener: () -> Unit) {
        listeners -= listener
    }

    fun transform(point: Vector2) = transform(point.x, point.y) { x, y -> point.set(x, y) }

    fun <R> transform(x: Float, y: Float, block: (Float, Float) -> R): R = global.transform(x, y, block)

    override fun read(properties: Properties) {
        properties.getFloatArray("position")?.let {
            x = it.getOrElse(0) { 0.0f }
            y = it.getOrElse(1) { 0.0f }
        }

        properties.getFloat("rotation")?.let {
            rotation = it
        }

        properties.getFloatArray("scale")?.let {
            scaleX = it.getOrElse(0) { 0.0f }
            scaleY = it.getOrElse(1) { 0.0f }
        }
    }

    override fun write(properties: Properties) {
        properties.setFloatArray("position", arrayOf(x, y))
        properties.setFloat("rotation", rotation)
        properties.setFloatArray("scale", arrayOf(scaleX, scaleY))
    }
}

fun Transform.rotateAround(x: Float, y: Float, angle: Float) {
    val s = sin(angle)
    val c = cos(angle)

    this.x -= x
    this.y -= y

    val nx = this.x * c - this.y * s
    val ny = this.x * s + this.y * c

    this.x = nx + x
    this.y = ny + y
}
