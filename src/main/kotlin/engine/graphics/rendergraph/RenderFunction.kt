package engine.graphics.rendergraph

import com.cozmicgames.graphics.gpu.Texture2D

abstract class RenderFunction {
    sealed class Dependency(val getNode: () -> RenderGraph.Node) {
        abstract val texture: Texture2D
    }

    class ColorRenderTargetDependency(getNode: () -> RenderGraph.Node, val index: Int) : Dependency(getNode) {
        override val texture get() = requireNotNull(getNode().pass.getColorTexture(index))
    }

    class DepthRenderTargetDependency(getNode: () -> RenderGraph.Node) : Dependency(getNode) {
        override val texture get() = requireNotNull(getNode().pass.getDepthTexture())
    }

    class StencilRenderTargetDependency(getNode: () -> RenderGraph.Node) : Dependency(getNode) {
        override val texture get() = requireNotNull(getNode().pass.getStencilTexture())
    }

    internal var graph: RenderGraph? = null

    lateinit var pass: RenderPass
        internal set

    val dependencies get() = internalDependencies as List<Dependency>

    private val internalDependencies = arrayListOf<Dependency>()

    fun <T : Dependency> registerDependency(dependency: T): T {
        internalDependencies += dependency
        return dependency
    }

    abstract fun render(delta: Float)

    open fun onResize(width: Int, height: Int) {}
}

fun RenderFunction.colorRenderTargetDependency(getNode: () -> RenderGraph.Node, index: Int) = registerDependency(RenderFunction.ColorRenderTargetDependency(getNode, index))

fun RenderFunction.depthRenderTargetDependency(getNode: () -> RenderGraph.Node) = registerDependency(RenderFunction.DepthRenderTargetDependency(getNode))

fun RenderFunction.stencilRenderTargetDependency(getNode: () -> RenderGraph.Node) = registerDependency(RenderFunction.StencilRenderTargetDependency(getNode))

fun RenderFunction.colorRenderTargetDependency(node: RenderGraph.Node, index: Int) = colorRenderTargetDependency({ node }, index)

fun RenderFunction.depthRenderTargetDependency(node: RenderGraph.Node) = depthRenderTargetDependency { node }

fun RenderFunction.stencilRenderTargetDependency(node: RenderGraph.Node) = stencilRenderTargetDependency { node }

fun RenderFunction.colorRenderTargetDependency(name: String, index: Int) = colorRenderTargetDependency({ requireNotNull(graph?.getNode(name)) }, index)

fun RenderFunction.depthRenderTargetDependency(name: String) = depthRenderTargetDependency { requireNotNull(graph?.getNode(name)) }

fun RenderFunction.stencilRenderTargetDependency(name: String) = stencilRenderTargetDependency { requireNotNull(graph?.getNode(name)) }
