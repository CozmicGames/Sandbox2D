package engine.graphics.font

import com.cozmicgames.graphics.Font
import com.cozmicgames.graphics.Image
import com.cozmicgames.graphics.gpu.Pipeline
import com.cozmicgames.graphics.gpu.Texture2D
import com.cozmicgames.graphics.gpu.getFloatUniform
import com.cozmicgames.graphics.gpu.update
import com.cozmicgames.graphics.toTexture2D
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.collections.Array2D
import com.cozmicgames.utils.extensions.clamp
import com.cozmicgames.utils.maths.distanceSquared
import com.cozmicgames.utils.rectpack.RectPacker
import engine.Game
import engine.graphics.font.DrawableFont.Companion.defaultChars
import engine.graphics.shaders.SDFShader
import engine.assets.managers.shaders
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class SDFFont(val font: Font, override val drawableCharacters: String = defaultChars(), padding: Int = 4, downscale: Int = 3, spread: Float = 3.0f, override val size: Float = 14.0f) : DrawableFont {
    companion object {
        init {
            Game.assets.shaders?.add("sdf", SDFShader)
        }
    }

    override val texture: Texture2D

    override val requiredShader = "sdf"

    private val glyphs = hashMapOf<Char, Glyph>()

    var smoothing = 0.5f
    var outlineSize = 0.0f
    val outlineColor = Color.WHITE.copy()

    init {
        val charImages = hashMapOf<Int, Image>()

        drawableCharacters.forEach {
            val image = font.getCharImage(it, size * downscale)

            charImages[it.code] = if (image != null) {
                val bitmap = Array2D<Boolean>(image.width, image.height)

                fun isInside(color: Color): Boolean {
                    val bits = color.abgrBits
                    return (bits and 0x808080) != 0 && (bits and 0x80000000.toInt()) != 0
                }

                fun distanceToColor(distance: Float): Color {
                    val alpha = 0.5f + 0.5f * distance / spread
                    return Color(1.0f, 1.0f, 1.0f, alpha.clamp(0.0f, 1.0f))
                }

                fun findSignedDistance(centerX: Int, centerY: Int): Float {
                    val base = bitmap[centerX, centerY] == true
                    val delta = ceil(spread).toInt()
                    val startX = max(0, centerX - delta)
                    val endX = min(bitmap.width - 1, centerX + delta)
                    val startY = max(0, centerY - delta)
                    val endY = min(bitmap.height - 1, centerY + delta)

                    var closestDistanceSquared = delta * delta

                    for (y in startY..endY)
                        for (x in startX..endX)
                            if (base != bitmap[x, y]) {
                                val squareDist = distanceSquared(centerX, centerY, x, y)
                                if (squareDist < closestDistanceSquared)
                                    closestDistanceSquared = squareDist
                            }

                    val closestDist = sqrt(closestDistanceSquared.toFloat())
                    return (if (base) 1 else -1) * min(closestDist, spread)
                }

                repeat(image.width) { x ->
                    repeat(image.height) { y ->
                        bitmap[x, y] = isInside(image[x, y])
                    }
                }

                val sdfImage = Image(image.width / downscale, image.height / downscale)

                repeat(sdfImage.width) { x ->
                    repeat(sdfImage.height) { y ->
                        val centerX = x * downscale + downscale / 2
                        val centerY = y * downscale + downscale / 2
                        val signedDistance = findSignedDistance(centerX, centerY)
                        sdfImage[x, y] = distanceToColor(signedDistance)
                    }
                }

                sdfImage
            } else
                requireNotNull(font.getCharImage(' ', size))
        }

        var image = Image(128, 128)

        val rects = charImages.map { (char, image) ->
            RectPacker.Rectangle(char, image.width + padding, image.height + padding)
        }.toTypedArray()

        while (true) {
            val packer = RectPacker(image.width, image.height)
            packer.pack(rects)

            if (rects.any { !it.isPacked }) {
                image = Image(image.width * 2, image.height * 2)
                continue
            }

            for (rect in rects) {
                val x = rect.x + padding / 2
                val y = rect.y + padding / 2

                val charImage = charImages[rect.id] ?: continue

                image.setImage(charImage, x, y, charImage.width, charImage.height)

                val u0 = x.toFloat() / image.width
                val v0 = y.toFloat() / image.height
                val u1 = (x + charImage.width).toFloat() / image.width
                val v1 = (y + charImage.height).toFloat() / image.height

                glyphs[rect.id.toChar()] = Glyph(u0, v0, u1, v1, charImage.width, charImage.height)
            }

            break
        }

        texture = image.toTexture2D(Game.graphics2d.linearClampSampler)
    }

    override operator fun get(char: Char) = glyphs.getOrElse(char) { requireNotNull(glyphs[' ']) }

    override fun setUniforms(pipeline: Pipeline) {
        pipeline.getFloatUniform("uSmoothing")?.update(smoothing)
        pipeline.getFloatUniform("uOutlineSize")?.update(outlineSize)
        pipeline.getFloatUniform("uOutlineColor")?.update(outlineColor)
    }

    override fun dispose() {
        texture.dispose()
    }
}