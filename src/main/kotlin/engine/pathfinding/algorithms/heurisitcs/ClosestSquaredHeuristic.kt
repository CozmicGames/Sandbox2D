package engine.pathfinding.algorithms.heurisitcs

import com.cozmicgames.utils.maths.lengthSquared
import engine.pathfinding.MovableObject
import engine.pathfinding.TileBasedMap
import engine.pathfinding.algorithms.AStarPathFinder

object ClosestSquaredHeuristic : AStarPathFinder.Heuristic {
    override fun getCost(map: TileBasedMap, x: Int, y: Int, targetX: Int, targetY: Int, movableObject: MovableObject?): Float {
        val dx = (targetX - x).toFloat()
        val dy = (targetY - y).toFloat()
        return lengthSquared(dx, dy)
    }
}