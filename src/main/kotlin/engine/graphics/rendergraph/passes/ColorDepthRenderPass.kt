package engine.graphics.rendergraph.passes

import com.cozmicgames.graphics.gpu.Texture
import engine.graphics.rendergraph.RenderPass
import engine.graphics.rendergraph.addDepthRenderTarget
import engine.graphics.rendergraph.standardResolution

class ColorDepthRenderPass(resolution: Resolution = standardResolution(), colorFormat: Texture.Format = Texture.Format.RGBA8_UNORM, depthFormat: Texture.Format = Texture.Format.DEPTH24) : RenderPass(resolution) {
    val color = addColorRenderTarget(colorFormat)
    val depth = addDepthRenderTarget(depthFormat)
}