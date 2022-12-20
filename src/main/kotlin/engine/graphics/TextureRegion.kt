package engine.graphics

import com.cozmicgames.graphics.gpu.Texture2D

data class TextureRegion(var texture: Texture2D, var u0: Float, var v0: Float, var u1: Float, var v1: Float) {
    constructor(texture: Texture2D) : this(texture, 0.0f, 0.0f, 1.0f, 1.0f)

    constructor(region: TextureRegion) : this(region.texture, region.u0, region.v0, region.u1, region.v1)

    constructor(region: TextureRegion, u0: Float, v0: Float, u1: Float, v1: Float) : this(region.getSubRegion(u0, v0, u1, v1))

    val regionX get() = u0 * texture.width

    val regionY get() = v0 * texture.height

    val width get() = (texture.width * (u1 - u0)).toInt()

    val height get() = (texture.height * (v1 - v0)).toInt()

    val uRange get() = u1 - u0

    val vRange get() = v1 - v0

    fun getLocalU(u: Float) = u0 + u * uRange

    fun getLocalV(v: Float) = v0 + v * vRange

    fun getSubRegion(u0: Float, v0: Float, u1: Float, v1: Float): TextureRegion {
        val x0 = regionX + u0 * width
        val y0 = regionY + v0 * height
        val x1 = regionX + u1 * width
        val y1 = regionY + v1 * height
        return TextureRegion(texture, x0 / texture.width, y0 / texture.height, x1 / texture.width, y1 / texture.height)
    }

    fun split(rows: Int, columns: Int): Array<TextureRegion> {
        val dU = 1.0f / columns
        val dV = 1.0f / rows

        return Array(rows * columns) {
            val x = it % columns
            val y = it / rows
            val u0 = x * dU
            val v0 = y * dV
            getSubRegion(u0, v0, u0 + dU, v0 + dV)
        }
    }
}

fun Texture2D.asRegion(u0: Float = 0.0f, v0: Float = 0.0f, u1: Float = 1.0f, v1: Float = 1.0f) = TextureRegion(this, u0, v0, u1, v1)
