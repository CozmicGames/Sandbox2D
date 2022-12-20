package engine.graphics.rendergraph.passes

import com.cozmicgames.graphics.gpu.Texture
import engine.graphics.rendergraph.RenderPass
import engine.graphics.rendergraph.standardResolution

class ColorRenderPass(resolution: Resolution = standardResolution(), colorFormat: Texture.Format = Texture.Format.RGBA8_UNORM) : RenderPass(resolution) {
    val color = addColorRenderTarget(colorFormat)
}