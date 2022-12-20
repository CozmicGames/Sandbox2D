package engine.graphics.ui.layout

import com.cozmicgames.Kore
import com.cozmicgames.graphics
import com.cozmicgames.utils.collections.Array2D

abstract class GUIConstraint {
    internal enum class Type {
        X,
        Y,
        WIDTH,
        HEIGHT
    }

    internal lateinit var type: Type

    abstract fun getValue(parent: GUIRegion?, child: GUIRegion): Float
}

fun absolute(value: Float, mirror: Boolean = false) = object : GUIConstraint() {
    override fun getValue(parent: GUIRegion?, child: GUIRegion) = when (type) {
        Type.X -> if (mirror) ((parent?.x ?: 0.0f) + (parent?.width ?: Kore.graphics.width.toFloat())) - (value + child.width) else (parent?.x ?: 0.0f) + value
        Type.Y -> if (mirror) ((parent?.y ?: 0.0f) + (parent?.height ?: Kore.graphics.height.toFloat())) - (value + child.height) else (parent?.y ?: 0.0f) + value
        Type.WIDTH, Type.HEIGHT -> value
    }
}

fun relative(value: Float) = object : GUIConstraint() {
    override fun getValue(parent: GUIRegion?, child: GUIRegion) = when (type) {
        Type.X -> (parent?.x ?: 0.0f) + (parent?.width ?: Kore.graphics.width.toFloat()) * value
        Type.Y -> (parent?.y ?: 0.0f) + (parent?.height ?: Kore.graphics.height.toFloat()) * value
        Type.WIDTH -> (parent?.width ?: Kore.graphics.width.toFloat()) * value
        Type.HEIGHT -> (parent?.height ?: Kore.graphics.height.toFloat()) * value
    }
}

fun center() = object : GUIConstraint() {
    override fun getValue(parent: GUIRegion?, child: GUIRegion) = when (type) {
        Type.X -> (parent?.x ?: 0.0f) + ((parent?.width ?: Kore.graphics.width.toFloat()) - child.width) * 0.5f
        Type.Y -> (parent?.y ?: 0.0f) + ((parent?.height ?: Kore.graphics.height.toFloat()) - child.height) * 0.5f
        else -> throw UnsupportedOperationException()
    }
}

fun aspect(value: Float = 1.0f) = object : GUIConstraint() {
    override fun getValue(parent: GUIRegion?, child: GUIRegion) = when (type) {
        Type.WIDTH -> child.height * value
        Type.HEIGHT -> child.width / value
        else -> throw UnsupportedOperationException()
    }
}

fun fill() = object : GUIConstraint() {
    override fun getValue(parent: GUIRegion?, child: GUIRegion) = when (type) {
        Type.WIDTH -> parent?.width ?: Kore.graphics.width.toFloat()
        Type.HEIGHT -> parent?.height ?: Kore.graphics.height.toFloat()
        else -> throw UnsupportedOperationException()
    }
}

fun add(a: GUIConstraint, b: GUIConstraint) = object : GUIConstraint() {
    override fun getValue(parent: GUIRegion?, child: GUIRegion): Float {
        a.type = type
        b.type = type
        return a.getValue(parent, child) + b.getValue(parent, child)
    }
}

fun subtract(a: GUIConstraint, b: GUIConstraint) = object : GUIConstraint() {
    override fun getValue(parent: GUIRegion?, child: GUIRegion): Float {
        a.type = type
        b.type = type
        return a.getValue(parent, child) - b.getValue(parent, child)
    }
}

fun multiply(a: GUIConstraint, b: GUIConstraint) = object : GUIConstraint() {
    override fun getValue(parent: GUIRegion?, child: GUIRegion): Float {
        a.type = type
        b.type = type
        return a.getValue(parent, child) * b.getValue(parent, child)
    }
}

fun divide(a: GUIConstraint, b: GUIConstraint) = object : GUIConstraint() {
    override fun getValue(parent: GUIRegion?, child: GUIRegion): Float {
        a.type = type
        b.type = type
        return a.getValue(parent, child) / b.getValue(parent, child)
    }
}

operator fun GUIConstraint.plus(other: GUIConstraint) = add(this, other)

operator fun GUIConstraint.minus(other: GUIConstraint) = subtract(this, other)

operator fun GUIConstraint.times(other: GUIConstraint) = multiply(this, other)

operator fun GUIConstraint.div(other: GUIConstraint) = divide(this, other)

class GUIConstraints {
    companion object {
        private val DEFAULT_X = absolute(0.0f).apply { type = GUIConstraint.Type.X }
        private val DEFAULT_Y = absolute(0.0f).apply { type = GUIConstraint.Type.Y }
        private val DEFAULT_WIDTH = fill().apply { type = GUIConstraint.Type.WIDTH }
        private val DEFAULT_HEIGHT = fill().apply { type = GUIConstraint.Type.HEIGHT }
    }

    var x: GUIConstraint = DEFAULT_X
        set(value) {
            value.type = GUIConstraint.Type.X
            field = value
        }

    var y: GUIConstraint = DEFAULT_Y
        set(value) {
            value.type = GUIConstraint.Type.Y
            field = value
        }

    var width: GUIConstraint = DEFAULT_WIDTH
        set(value) {
            value.type = GUIConstraint.Type.WIDTH
            field = value
        }

    var height: GUIConstraint = DEFAULT_HEIGHT
        set(value) {
            value.type = GUIConstraint.Type.HEIGHT
            field = value
        }
}

fun GUIConstraints.split(rows: Int, cols: Int): Array2D<GUIConstraints> {
    val result = Array2D(rows, cols) { _, _ -> GUIConstraints() }

    val rowHeight = 1.0f / rows
    val colWidth = 1.0f / cols

    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val rowConstraints = result[row, col]
            rowConstraints?.x = relative(col * colWidth)
            rowConstraints?.y = relative(row * rowHeight)
            rowConstraints?.width = relative(colWidth)
            rowConstraints?.height = relative(rowHeight)
        }
    }
    return result
}
