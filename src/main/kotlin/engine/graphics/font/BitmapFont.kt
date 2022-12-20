package engine.graphics.font

import com.cozmicgames.graphics.Font
import com.cozmicgames.graphics.Image
import com.cozmicgames.graphics.gpu.Texture2D
import com.cozmicgames.graphics.toTexture2D
import com.cozmicgames.utils.rectpack.RectPacker
import engine.Game
import engine.graphics.font.DrawableFont.Companion.defaultChars

class BitmapFont(val font: Font, override val drawableCharacters: String = defaultChars(), padding: Int = 4, override val size: Float = 14.0f) : DrawableFont {
    override val texture: Texture2D

    private val glyphs = hashMapOf<Char, Glyph>()

    init {
        val charImages = hashMapOf<Int, Image>()

        drawableCharacters.forEach {
            charImages[it.code] = font.getCharImage(it, size) ?: requireNotNull(font.getCharImage(' ', size))
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

        texture = image.toTexture2D(Game.graphics2d.pointClampSampler)
    }

    override operator fun get(char: Char) = glyphs.getOrElse(char) { requireNotNull(glyphs[' ']) }

    override fun dispose() {
        texture.dispose()
    }
}