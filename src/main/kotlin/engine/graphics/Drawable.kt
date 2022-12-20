package engine.graphics

interface Drawable {
    data class Vertex(var x: Float = 0.0f, var y: Float = 0.0f, var u: Float = 0.0f, var v: Float = 0.0f)

    val vertices: Array<Vertex>
    val indices: Array<Int>

    val verticesCount get() = vertices.size
    val indicesCount get() = indices.size

    val material: String? get() = null
    val layer: Int get() = 0
}

class DrawableBuilder {
    private val vertices = arrayListOf<Drawable.Vertex>()
    private val indices = arrayListOf<Int>()

    fun index(index: Int) {
        indices += index
    }

    fun vertex(block: Drawable.Vertex.() -> Unit) {
        val vertex = Drawable.Vertex()
        block(vertex)
        vertices += vertex
    }

    fun build(material: String, layer: Int) = object : Drawable {
        override val vertices = this@DrawableBuilder.vertices.toTypedArray()
        override val indices = this@DrawableBuilder.indices.toTypedArray()

        override val layer get() = layer
        override val material get() = material
    }
}

fun buildDrawable(material: String, layer: Int, block: DrawableBuilder.() -> Unit): Drawable {
    val builder = DrawableBuilder()
    block(builder)
    return builder.build(material, layer)
}
