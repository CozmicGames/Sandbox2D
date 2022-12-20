package engine.pathfinding

import com.cozmicgames.utils.maths.Vector2i

interface PathFinder {
    fun findPath(movableObject: MovableObject?, startX: Int, startY: Int, targetX: Int, targetY: Int, maxSearchDistance: Int, allowDiagonalMovement: Boolean): Path?
}

fun PathFinder.findPath(movableObject: MovableObject?, start: Vector2i, target: Vector2i, maxSearchDistance: Int = Int.MAX_VALUE, allowDiagonalMovement: Boolean = true) = findPath(movableObject, start.x, start.y, target.x, target.y, maxSearchDistance, allowDiagonalMovement)
