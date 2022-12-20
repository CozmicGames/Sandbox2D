package game.level.ui

import com.cozmicgames.utils.collections.Array2D

class SetTilesCommand(region: GridRegion, private val tiles: Array2D<String?>) : EditorCommandExecutor.Command {
    override val isUndoable get() = true

    private val region = region.copy()
    private val previousTiles = region.getTiles()

    override fun execute(): Boolean {
        region.setTiles(tiles)
        return true
    }

    override fun undo() {
        region.setTiles(previousTiles)
    }
}
