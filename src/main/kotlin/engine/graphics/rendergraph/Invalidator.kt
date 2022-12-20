package engine.graphics.rendergraph

class Invalidator {
    internal var node: RenderGraph.Node.OnInvalid? = null

    fun invalidate() {
        node?.setDirty()
    }
}