package engine.graphics.rendergraph.passes

import com.cozmicgames.graphics.gpu.Texture
import engine.graphics.rendergraph.RenderPass
import engine.graphics.rendergraph.addDepthRenderTarget
import engine.graphics.rendergraph.standardResolution

class DepthOnlyRenderPass(resolution: Resolution = standardResolution(), depthFormat: Texture.Format = Texture.Format.DEPTH24) : RenderPass(resolution) {
    val depth = addDepthRenderTarget(depthFormat)
}