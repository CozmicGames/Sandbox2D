package game.components

import com.cozmicgames.utils.Properties
import com.cozmicgames.utils.Updateable
import engine.Game
import engine.graphics.buildDrawable
import engine.scene.Component
import engine.scene.GameObject
import engine.scene.components.DrawableProviderComponent
import engine.scene.components.TransformComponent
import engine.scene.findGameObjectInChildren
import engine.assets.managers.getTileSet
import game.level.GridDrawable

class GridComponent : Component(), Updateable {
    companion object {
        private const val CHUNK_SIZE = 32
    }

    private inner class Chunk(val chunkX: Int, val chunkY: Int) {
        var isDirty = true

        fun rebuildDrawables() {
            removeDrawables()

            val tileSet = Game.assets.getTileSet(tileSet) ?: return

            val batches = hashMapOf<String, MutableList<GridCellComponent>>()

            val minTileX = chunkX * CHUNK_SIZE
            val minTileY = chunkY * CHUNK_SIZE
            val maxTileX = minTileX + CHUNK_SIZE
            val maxTileY = minTileY + CHUNK_SIZE

            fun findCellComponents(gameObject: GameObject) {
                for (child in gameObject.children) {
                    val gridCellComponent = child.getComponent<GridCellComponent>() ?: continue

                    if (gridCellComponent.cellX !in minTileX until maxTileX || gridCellComponent.cellY !in minTileY until maxTileY)
                        continue

                    val material = tileSet[gridCellComponent.tileType]?.getMaterial(this@GridComponent, gridCellComponent.cellX, gridCellComponent.cellY)

                    batches.getOrPut(material ?: "<missing>") { arrayListOf() } += gridCellComponent
                }
            }

            findCellComponents(gameObject)

            batches.forEach { (material, list) ->
                val drawable = buildDrawable(material, layer) {
                    var currentIndex = 0

                    list.forEach {
                        val posX = it.cellX * cellSize + transformComponent.transform.x
                        val posY = it.cellY * cellSize + transformComponent.transform.y

                        vertex {
                            x = posX
                            y = posY
                            u = 0.0f
                            v = 1.0f
                        }

                        vertex {
                            x = posX + cellSize
                            y = posY
                            u = 1.0f
                            v = 1.0f
                        }

                        vertex {
                            x = posX + cellSize
                            y = posY + cellSize
                            u = 1.0f
                            v = 0.0f
                        }

                        vertex {
                            x = posX
                            y = posY + cellSize
                            u = 0.0f
                            v = 0.0f
                        }

                        index(currentIndex)
                        index(currentIndex + 1)
                        index(currentIndex + 2)
                        index(currentIndex)
                        index(currentIndex + 2)
                        index(currentIndex + 3)

                        currentIndex += 4
                    }
                }

                if (drawable.verticesCount > 0 && drawable.indicesCount > 0)
                    drawableProviderComponent.drawables += GridDrawable(chunkX, chunkY, drawable)
            }
        }

        fun removeDrawables() {
            drawableProviderComponent.drawables.removeIf { it is GridDrawable && it.chunkX == chunkX && it.chunkY == chunkY }
        }
    }

    var cellSize = 32.0f
    var tileSet = "<missing>"
    var isCollidable = false
    var layer = 0

    private val chunks = arrayListOf<Chunk>()

    private lateinit var drawableProviderComponent: DrawableProviderComponent
    private lateinit var transformComponent: TransformComponent

    fun setDirty(cellX: Int, cellY: Int) {
        val chunkX = (if (cellX < 0) cellX - CHUNK_SIZE - 1 else cellX) / CHUNK_SIZE
        val chunkY = (if (cellY < 0) cellY - CHUNK_SIZE - 1 else cellY) / CHUNK_SIZE

        var chunk = chunks.find { it.chunkX == chunkX && it.chunkY == chunkY }

        if (chunk == null) {
            chunk = Chunk(chunkX, chunkY)
            chunks += chunk
        }

        chunk.isDirty = true
    }

    override fun onAdded() {
        drawableProviderComponent = gameObject.getOrAddComponent()
        transformComponent = gameObject.getOrAddComponent()
        transformComponent.transform.addChangeListener {
            chunks.forEach {
                it.isDirty = true
            }
        }
    }

    override fun update(delta: Float) {
        chunks.forEach {
            if (it.isDirty)
                it.rebuildDrawables()
        }
    }

    override fun onRemoved() {
        chunks.forEach {
            it.removeDrawables()
        }
    }

    override fun read(properties: Properties) {
        properties.getFloat("cellSize")?.let { cellSize = it }
        properties.getString("tileSet")?.let { tileSet = it }
        properties.getBoolean("isCollidable")?.let { isCollidable = it }
        properties.getInt("layer")?.let { layer = it }
    }

    override fun write(properties: Properties) {
        properties.setFloat("cellSize", cellSize)
        properties.setString("tileSet", tileSet)
        properties.setBoolean("isCollidable", isCollidable)
        properties.setInt("layer", layer)
    }

    fun getCellObject(cellX: Int, cellY: Int): GameObject? {
        return gameObject.findGameObjectInChildren {
            it.getComponent<GridCellComponent>()?.let { it.cellX == cellX && it.cellY == cellY } == true
        }
    }

    fun removeCellObject(cellX: Int, cellY: Int) {
        val cellObject = getCellObject(cellX, cellY) ?: return
        gameObject.scene.removeGameObject(cellObject)
    }

    fun getOrAddCellObject(cellX: Int, cellY: Int): GameObject {
        var cellObject = getCellObject(cellX, cellY)

        if (cellObject == null) {
            cellObject = gameObject.scene.addGameObject {
                addComponent<GridCellComponent> {
                    this.cellX = cellX
                    this.cellY = cellY
                }
            }

            cellObject.parent = gameObject
        }

        return cellObject
    }
}

fun GridComponent.getCellType(cellX: Int, cellY: Int) = getCellObject(cellX, cellY)?.getComponent<GridCellComponent>()?.tileType

fun GridComponent.getCells() = buildList {
    fun check(gameObject: GameObject) {
        val gridCellComponent = gameObject.getComponent<GridCellComponent>()

        if (gridCellComponent != null)
            add(gridCellComponent)

        gameObject.children.forEach(::check)
    }

    gameObject.children.forEach(::check)
}
