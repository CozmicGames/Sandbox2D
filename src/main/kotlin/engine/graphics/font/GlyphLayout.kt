package engine.graphics.font

import com.cozmicgames.utils.collections.Pool
import com.cozmicgames.utils.maths.Rectangle
import com.cozmicgames.utils.maths.Vector2
import kotlin.math.max

class GlyphLayout() : Iterable<GlyphLayout.Quad> {
    constructor(text: String, font: DrawableFont, lineGap: Float = 1.0f, bounds: Rectangle? = null, hAlign: HAlign = HAlign.LEFT, vAlign: VAlign = VAlign.TOP) : this() {
        update(text, font, lineGap, bounds, hAlign, vAlign)
    }

    class Quad : Rectangle() {
        var u0 = 0.0f
        var v0 = 0.0f
        var u1 = 0.0f
        var v1 = 0.0f
    }

    private val quads = arrayListOf<Quad>()
    private val quadPool = Pool(supplier = { Quad() })

    val count get() = quads.size

    var lineCount = 0
        private set

    var lineHeight = 0.0f
        private set

    var width = 0.0f
        private set

    var height = 0.0f
        private set

    lateinit var font: DrawableFont
        private set

    fun update(text: String, font: DrawableFont, lineGap: Float = 1.0f, bounds: Rectangle? = null, hAlign: HAlign = HAlign.LEFT, vAlign: VAlign = VAlign.TOP) {
        quads.forEach {
            quadPool.free(it)
        }
        quads.clear()

        lineCount = 0
        width = 0.0f
        height = 0.0f
        this.font = font
        lineHeight = lineGap * font.size

        if (bounds != null) {
            val lines = arrayListOf<String>()
            text.lineSequence().forEach {
                val lineWidth = it.sumOf { font[it].width }

                if (lineWidth < bounds.width) {
                    lines += it
                    lineCount++
                } else {
                    val words = it.split(" ")
                    val lineBuilder = StringBuilder()
                    var width = 0.0f

                    lineBuilder.append(words[0])

                    if (words.size > 1)
                        for (word in words.subList(1, words.size)) {
                            val wordWidth = word.sumOf { font[it].width }

                            if (width + wordWidth < bounds.width) {
                                lineBuilder.append(" ")
                                lineBuilder.append(word)
                                width += wordWidth
                            } else {
                                lines += lineBuilder.toString()
                                lineBuilder.clear()
                                lineCount++
                                width = 0.0f
                            }
                        }

                    lines += lineBuilder.toString()
                }
            }

            var y = 0.0f

            lines.forEach {
                val lineWidth = it.sumOf { font[it].width }

                var x = when (hAlign) {
                    HAlign.LEFT -> 0.0f
                    HAlign.CENTER -> (bounds.width - lineWidth) * 0.5f
                    HAlign.RIGHT -> bounds.width - lineWidth
                }

                it.forEach {
                    val glyph = font[it]

                    val quad = quadPool.obtain()
                    quad.x = x
                    quad.y = y
                    quad.width = glyph.width.toFloat()
                    quad.height = glyph.height.toFloat()
                    quad.u0 = glyph.u0
                    quad.v0 = glyph.v0
                    quad.u1 = glyph.u1
                    quad.v1 = glyph.v1
                    quads += quad

                    x += glyph.width
                }

                lineCount++
                y += lineHeight
            }

            width = lines.maxByOrNull { it.sumOf { font[it].width } }?.toFloat() ?: 0.0f
            height = y

            val yOffset = when (vAlign) {
                VAlign.TOP -> 0.0f
                VAlign.MIDDLE -> (bounds.height - height) * 0.5f
                VAlign.BOTTOM -> bounds.height - height
            }

            quads.forEach {
                it.y += yOffset
            }
        } else {
            var y = 0.0f
            text.lineSequence().forEach {
                var x = 0.0f

                it.forEach {
                    val glyph = font[it]

                    val quad = quadPool.obtain()
                    quad.x = x
                    quad.y = y
                    quad.width = glyph.width.toFloat()
                    quad.height = glyph.height.toFloat()
                    quad.u0 = glyph.u0
                    quad.v0 = glyph.v0
                    quad.u1 = glyph.u1
                    quad.v1 = glyph.v1
                    quads += quad

                    x += glyph.width
                }

                width = max(width, x)

                lineCount++
                y += lineHeight
            }

            height = y
        }
    }

    override fun iterator() = quads.iterator()

    operator fun get(index: Int) = quads[index]

    fun findCursorIndex(x: Float, y: Float) = findCursorIndex(Vector2(x, y))

    fun findCursorIndex(point: Vector2): Int {
        for ((index, quad) in quads.withIndex()) {
            if (point in quad)
                return index

            if (index == quads.lastIndex) {
                val rectangle = Rectangle()
                rectangle.x = quad.maxX
                rectangle.y = quad.y
                rectangle.width = quad.width * 2.0f
                rectangle.height = quad.height

                if (point in rectangle)
                    return index + 1
            }
        }
        return -1
    }
}