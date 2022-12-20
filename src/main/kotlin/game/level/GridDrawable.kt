package game.level

import engine.graphics.Drawable

class GridDrawable(val chunkX: Int, val chunkY: Int, private val drawable: Drawable) : Drawable {
    override val indices by drawable::indices
    override val vertices by drawable::vertices
    override val material by drawable::material
    override val layer by drawable::layer
}