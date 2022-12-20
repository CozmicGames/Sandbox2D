package engine.pathfinding

data class PathFindingContext(val startX: Int, val startY: Int, val movableObject: MovableObject?) {
    var searchDistance = 0
}