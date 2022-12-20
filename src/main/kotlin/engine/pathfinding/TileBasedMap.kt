package engine.pathfinding

interface TileBasedMap {
    val numTilesX: Int
    val numTilesY: Int

    fun isBlocked(x: Int, y: Int, sourceX: Int, sourceY: Int, context: PathFindingContext): Boolean

    fun getCost(x: Int, y: Int, sourceX: Int, sourceY: Int, context: PathFindingContext): Float
}