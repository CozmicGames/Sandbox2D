package engine.pathfinding.algorithms.heurisitcs

import engine.pathfinding.MovableObject
import engine.pathfinding.TileBasedMap
import engine.pathfinding.algorithms.AStarPathFinder
import kotlin.math.abs

class ManhattanHeuristic(var minimumCost: Float = 1.0f) : AStarPathFinder.Heuristic {
    override fun getCost(map: TileBasedMap, x: Int, y: Int, targetX: Int, targetY: Int, movableObject: MovableObject?): Float {
        val dx = x - targetX
        val dy = y - targetY
        return minimumCost * (abs(dx) + abs(dy))
    }
}