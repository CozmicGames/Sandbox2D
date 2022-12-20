package game.level.ui

import com.cozmicgames.utils.collections.Array2D
import game.components.GridCellComponent
import game.components.GridComponent
import game.components.getCellType
import kotlin.math.max
import kotlin.math.min

class GridRegion(val grid: GridComponent, var x0: Int, var y0: Int, var x1: Int, var y1: Int) {
    val minX get() = min(x0, x1)
    val minY get() = min(y0, y1)
    val maxX get() = max(x0, x1)
    val maxY get() = max(y0, y1)

    val width get() = maxX - minX + 1
    val height get() = maxY - minY + 1

    fun getTiles(): Array2D<String?> {
        return Array2D(width, height) { x, y ->
            grid.getCellType(x + minX, y + minY)
        }
    }

    fun setTiles(width: Int = this.width, height: Int = this.height, getTile: (Int, Int) -> String?) {
        val usedWidth = min(width, this.width)
        val usedHeight = min(height, this.height)

        repeat(usedWidth) { x ->
            repeat(usedHeight) { y ->
                val type = getTile(x, y)
                if (type == null)
                    grid.removeCellObject(x + minX, y + minY)
                else
                    grid.getOrAddCellObject(x + minX, y + minY).getOrAddComponent<GridCellComponent>().tileType = type
            }
        }
    }

    fun setTiles(tiles: Array2D<String?>) {
        setTiles(tiles.width, tiles.height) { x, y ->
            tiles[x, y]
        }
    }

    fun setTiles(width: Int = this.width, height: Int = this.height, tile: String?) {
        setTiles(width, height) { _, _ ->
            tile
        }
    }

    fun copy(grid: GridComponent = this.grid, x0: Int = this.x0, y0: Int = this.y0, x1: Int = this.x1, y1: Int = this.y1): GridRegion {
        return GridRegion(grid, x0, y0, x1, y1)
    }
}

fun GridComponent.getRegion(x: Int, y: Int, width: Int, height: Int) = GridRegion(this, x, y, x + width, y + height)
