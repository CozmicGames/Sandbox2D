package engine.graphics.font

import com.cozmicgames.graphics.gpu.Pipeline
import com.cozmicgames.graphics.gpu.Texture2D
import com.cozmicgames.utils.Disposable

interface DrawableFont : Disposable {
    companion object {
        fun defaultChars() = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890 \"!`?'.,;:()[]{}<>|/@\\^\$-%+=#_&~*"
    }

    val drawableCharacters: String
    val size: Float

    val requiredShader: String get() = "default"
    val texture: Texture2D

    operator fun get(char: Char): Glyph

    fun setUniforms(pipeline: Pipeline) {}
}