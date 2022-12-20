package engine.graphics

import com.cozmicgames.Kore
import com.cozmicgames.graphics
import com.cozmicgames.graphics.Image
import com.cozmicgames.graphics.gpu.Sampler
import com.cozmicgames.graphics.gpu.Texture
import com.cozmicgames.graphics.setImage
import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.rectpack.RectPacker
import engine.Game

class TextureAtlas(format: Texture.Format = Texture.Format.RGBA8_UNORM, sampler: Sampler = Game.graphics2d.pointClampSampler) : Disposable {
    val texture = Kore.graphics.createTexture2D(format, sampler)

    private var images = hashMapOf<String, Image>()
    private var packed = hashMapOf<String, TextureRegion>()
    private var isDirty = true

    operator fun get(name: String): TextureRegion? {
        if (isDirty) {
            pack()
            isDirty = false
        }
        return packed[name]
    }

    fun add(name: String, image: Image) = add(name to image)

    fun add(vararg images: Pair<String, Image>) = add(mapOf(*images))

    fun add(images: Map<String, Image>) {
        this.images.putAll(images)
        isDirty = true
    }

    fun remove(vararg names: String) {
        names.forEach {
            images.remove(it)
        }
        isDirty = false
    }

    fun pack() {
        packed.clear()

        val ids = arrayOfNulls<String>(images.size)
        var currentId = 0
        val rects = images.map { (name, image) ->
            val id = currentId++
            ids[id] = name
            RectPacker.Rectangle(id, image.width + 2, image.height + 2)
        }.toTypedArray()

        var image = Image(128, 128)

        while (true) {
            val packer = RectPacker(image.width, image.height)
            packer.pack(rects)

            if (rects.any { !it.isPacked }) {
                image = Image(image.width * 2, image.height * 2)
                continue
            }

            for (rect in rects) {
                val x = rect.x + 1
                val y = rect.y + 1

                val name = ids[rect.id] ?: continue
                val subImage = images[name] ?: continue

                val cornerMinX = x - 1
                val cornerMinY = y - 1
                val cornerMaxX = x + subImage.width
                val cornerMaxY = y + subImage.height

                image[cornerMinX, cornerMinY] = subImage[0, 0]
                image[cornerMaxX, cornerMinY] = subImage[subImage.width - 1, 0]
                image[cornerMaxX, cornerMaxY] = subImage[subImage.width - 1, subImage.height - 1]
                image[cornerMinX, cornerMaxY] = subImage[0, subImage.height - 1]

                image.drawImage(subImage, cornerMinX + 1, cornerMinY, height = 1)
                image.drawImage(subImage, cornerMinX + 1, cornerMaxY, srcY = subImage.height - 1, height = 1)
                image.drawImage(subImage, cornerMinX, cornerMinY + 1, width = 1)
                image.drawImage(subImage, cornerMaxX, cornerMinY + 1, srcX = subImage.width - 1, width = 1)
                image.drawImage(subImage, x, y)

                val u0 = x.toFloat() / image.width
                val v0 = y.toFloat() / image.height
                val u1 = (x + subImage.width).toFloat() / image.width
                val v1 = (y + subImage.height).toFloat() / image.height

                packed[name] = TextureRegion(texture, u0, v0, u1, v1)
            }

            break
        }

        texture.setImage(image)
    }

    override fun dispose() {
        texture.dispose()
    }
}