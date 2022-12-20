package engine.utils

import com.cozmicgames.graphics.Image
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.extensions.clamp
import kotlin.math.floor

class GradientColor(val color: Color, var stop: Float)

infix fun Color.at(stop: Float) = GradientColor(this, stop)

class Gradient(val color0: Color, val color1: Color, vararg colors: GradientColor) : Iterable<GradientColor> {
    var cacheSize = 256
        set(value) {
            if (field != value) {
                field = value
                cache = Array(value) { Color.WHITE.copy() }
                update()
            }
        }

    private val colors = arrayListOf<GradientColor>()
    private var cache = Array(cacheSize) { Color.WHITE.copy() }

    init {
        add(color0 at 0.0f)
        colors.forEach {
            add(it)
        }
        add(color1 at 1.0f)
        update()
    }

    override fun iterator() = colors.iterator()

    operator fun get(amount: Float): Color {
        val index = floor(amount.clamp(0.0f, 1.0f) * cache.size).toInt()
        return cache[index]
    }

    fun add(color: Color, stop: Float) = add(color at stop)

    fun add(color: GradientColor) {
        colors += color
        update()
    }

    fun update() {
        fun getBefore(stop: Float): GradientColor {
            var result = color0 at 0.0f

            for (i in colors.indices) {
                val color = colors[i]

                if (color.stop < stop)
                    result = color
            }

            return result
        }

        fun getAfter(stop: Float): GradientColor {
            var result = color1 at 1.0f

            for (i in colors.indices.reversed()) {
                val color = colors[i]

                if (color.stop >= stop)
                    result = color
            }

            return result
        }

        colors.sortBy { it.stop }

        val stepSize = 1.0f / cacheSize

        repeat(cacheSize) {
            val amount = it * stepSize

            val colorBefore = getBefore(amount)
            val colorAfter = getAfter(amount)

            val amountColorBefore = (colorAfter.stop - amount) / (colorAfter.stop - colorBefore.stop)
            val amountColorAfter = 1.0f - amountColorBefore

            with(cache[it]) {
                r = colorBefore.color.r * amountColorBefore + colorAfter.color.r * amountColorAfter
                g = colorBefore.color.g * amountColorBefore + colorAfter.color.g * amountColorAfter
                b = colorBefore.color.b * amountColorBefore + colorAfter.color.b * amountColorAfter
                a = colorBefore.color.a * amountColorBefore + colorAfter.color.a * amountColorAfter
            }
        }
    }
}

fun Gradient.toImage(image: Image = Image(cacheSize, 1)) {
    repeat(image.width) {
        val color = get(it.toFloat() / (image.width - 1))
        image[it, 0] = color
    }
}
