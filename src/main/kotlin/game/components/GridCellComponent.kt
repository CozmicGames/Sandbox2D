package game.components

import com.cozmicgames.utils.Properties
import com.cozmicgames.utils.maths.Rectangle
import engine.scene.Component
import engine.scene.components.TransformComponent
import engine.scene.findComponentInParentObjects
import game.level.GridCollidable

class GridCellComponent : Component(), GridCollidable {
    var cellX = 0
    var cellY = 0

    var tileType = "<missing>"

    override fun calculateBounds(bounds: Rectangle) {
        val gridComponent = gameObject.findComponentInParentObjects<GridComponent>() ?: return
        val transformComponent = gridComponent.gameObject.getComponent<TransformComponent>()

        bounds.x = cellX * gridComponent.cellSize
        bounds.y = cellY * gridComponent.cellSize
        bounds.width = gridComponent.cellSize
        bounds.height = gridComponent.cellSize

        transformComponent?.transform?.let {
            bounds.x += it.x
            bounds.y += it.y
        }
    }

    override fun onAdded() {
        val gridComponent = gameObject.findComponentInParentObjects<GridComponent>() ?: return
        gridComponent.setDirty(cellX, cellY)
    }

    override fun onRemoved() {
        val gridComponent = gameObject.findComponentInParentObjects<GridComponent>() ?: return
        gridComponent.setDirty(cellX, cellY)
    }

    override fun read(properties: Properties) {
        properties.getInt("cellX")?.let { cellX = it }
        properties.getInt("cellY")?.let { cellY = it }
        properties.getString("tileType")?.let { tileType = it }
    }

    override fun write(properties: Properties) {
        properties.setInt("cellX", cellX)
        properties.setInt("cellY", cellY)
        properties.setString("tileType", tileType)
    }
}
