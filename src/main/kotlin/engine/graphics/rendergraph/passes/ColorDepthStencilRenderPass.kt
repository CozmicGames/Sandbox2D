package engine.graphics.rendergraph.passes

import com.cozmicgames.graphics.gpu.Texture
import engine.graphics.rendergraph.RenderPass
import engine.graphics.rendergraph.addDepthRenderTarget
import engine.graphics.rendergraph.addStencilRenderTarget
import engine.graphics.rendergraph.standardResolution

class ColorDepthStencilRenderPass(resolution: Resolution = standardResolution(), colorFormat: Texture.Format = Texture.Format.RGBA8_UNORM, depthFormat: Texture.Format = Texture.Format.DEPTH24, stencilFormat: Texture.Format = Texture.Format.STENCIL8) : RenderPass(resolution) {
    val color = addColorRenderTarget(colorFormat)
    val depth = addDepthRenderTarget(depthFormat)
    val stencil = addStencilRenderTarget(stencilFormat)
}