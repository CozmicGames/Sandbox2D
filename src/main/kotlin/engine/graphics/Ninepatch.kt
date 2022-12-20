package engine.graphics

import com.cozmicgames.graphics.gpu.Texture2D

class Ninepatch(val texture: TextureRegion, left: Float = 1.0f / 3.0f, right: Float = 1.0f / 3.0f, top: Float = 1.0f / 3.0f, bottom: Float = 1.0f / 3.0f) {
    constructor(texture: Texture2D, left: Float = 1.0f / 3.0f, right: Float = 1.0f / 3.0f, top: Float = 1.0f / 3.0f, bottom: Float = 1.0f / 3.0f) : this(texture.asRegion(), left, right, top, bottom)

    var left = left
        set(value) {
            field = value
            isDirty = true
        }

    var right = right
        set(value) {
            field = value
            isDirty = true
        }

    var top = top
        set(value) {
            field = value
            isDirty = true
        }

    var bottom = bottom
        set(value) {
            field = value
            isDirty = true
        }

    private var isDirty = true
    private val regions = Array(9) { getPatchTexture(it) }

    private fun getIndex(x: Int, y: Int): Int {
        require(x >= 0 && x < 3 && y >= 0 && y < 3)
        return x + y * 3
    }

    private fun getPatchTexture(index: Int): TextureRegion {
        val x = index % 3
        val y = index / 3

        fun f(u0: Float, u1: Float) = when (y) {
            0 -> {
                val v0 = 0.0f
                val v1 = top
                TextureRegion(texture, u0, v0, u1, v1)
            }
            1 -> {
                val v0 = left
                val v1 = 1.0f - right
                TextureRegion(texture, u0, v0, u1, v1)
            }
            2 -> {
                val v0 = 1.0f - right
                val v1 = 1.0f
                TextureRegion(texture, u0, v0, u1, v1)
            }
            else -> throw Exception("y needs to be in the range 0..2")
        }

        return when (x) {
            0 -> {
                val u0 = 0.0f
                val u1 = left
                f(u0, u1)
            }
            1 -> {
                val u0 = left
                val u1 = 1.0f - right
                f(u0, u1)
            }
            2 -> {
                val u0 = 1.0f - right
                val u1 = 1.0f
                f(u0, u1)
            }
            else -> throw Exception("x needs to be in the range 0..2")
        }
    }

    fun update() {
        for (i in regions.indices)
            regions[i] = getPatchTexture(i)
        isDirty = false
    }

    operator fun get(x: Int, y: Int): TextureRegion {
        if (isDirty)
            update()
        return regions[getIndex(x, y)]
    }
}