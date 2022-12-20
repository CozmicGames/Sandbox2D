package engine.graphics.rendergraph.passes

import com.cozmicgames.graphics.gpu.Texture
import engine.graphics.rendergraph.RenderPass
import engine.graphics.rendergraph.addDepthRenderTarget
import engine.graphics.rendergraph.standardResolution

class MultiColorDepthRenderPass(resolution: Resolution = standardResolution(), colorFormats: Array<Texture.Format> = arrayOf(Texture.Format.RGBA8_UNORM), depthFormat: Texture.Format = Texture.Format.DEPTH24) : RenderPass(resolution) {
    val colors = Array(colorFormats.size) {
        addColorRenderTarget(colorFormats[it])
    }
    val depth = addDepthRenderTarget(depthFormat)
}