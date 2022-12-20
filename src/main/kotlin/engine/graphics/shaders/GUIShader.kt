package engine.graphics.shaders

import com.cozmicgames.Kore
import com.cozmicgames.graphics
import com.cozmicgames.graphics.Image
import com.cozmicgames.graphics.gpu.*
import com.cozmicgames.graphics.setImage
import com.cozmicgames.utils.Disposable
import engine.Game
import engine.graphics.Renderer
import engine.graphics.ui.GUIPaint
import engine.utils.toImage

object GUIShader : Shader(
    """
    #section state
    blend add source_alpha one_minus_source_alpha
    
    #section uniforms
    bool uHasGradient
    sampler2D uGradientTexture
    vec2 uGradientDirection
    
    #section common
    void vertexShader(inout Vertex v) {
    }
    
    vec4 fragmentShader(inout Fragment f) {
        if (uHasGradient) {
            vec4 color = texture(uGradientTexture, f.texcoord * uGradientDirection);
            f.color *= color;
        }
    
        return f.textureColor * f.color;
    }
""".trimIndent()
), Disposable {
    val gradientTexture = Kore.graphics.createTexture2D(Texture.Format.RGBA8_UNORM, Game.graphics2d.linearClampSampler)
    val gradientImage = Image(256, 1)

    init {
        Kore.addShutdownListener {
            dispose()
        }
    }

    override fun dispose() {
        gradientTexture.dispose()
    }
}

fun setPaint(paint: GUIPaint, pipeline: Pipeline) {
    pipeline.getBooleanUniform("uHasGradient")?.update(paint.gradient != null)

    paint.gradient?.let {
        it.toImage(GUIShader.gradientImage)
        GUIShader.gradientTexture.setImage(GUIShader.gradientImage)

        pipeline.getTexture2DUniform("uGradientTexture")?.update(GUIShader.gradientTexture)
        pipeline.getFloatUniform("uGradientDirection")?.update(paint.gradientDirection)
    }
}

fun <R> Renderer.withPaint(paint: GUIPaint, block: () -> R) = withShader(GUIShader) {
    val pipeline = getPipeline(GUIShader)

    pipeline?.getBooleanUniform("uHasGradient")?.update(paint.gradient != null)

    paint.gradient?.let {
        it.toImage(GUIShader.gradientImage)
        GUIShader.gradientTexture.setImage(GUIShader.gradientImage)

        pipeline?.getTexture2DUniform("uGradientTexture")?.update(GUIShader.gradientTexture)
        pipeline?.getFloatUniform("uGradientDirection")?.update(paint.gradientDirection)
    }

    block()
}
