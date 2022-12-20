package engine.pathfinding.algorithms

import com.cozmicgames.utils.collections.Array2D
import com.cozmicgames.utils.collections.PriorityList
import com.cozmicgames.utils.concurrency.threadLocal
import engine.pathfinding.*
import engine.pathfinding.algorithms.heurisitcs.ClosestHeuristic
import kotlin.math.max

class AStarPathFinder(val map: TileBasedMap, val heuristic: Heuristic = ClosestHeuristic) : PathFinder {
    interface Heuristic {
        fun getCost(map: TileBasedMap, x: Int, y: Int, targetX: Int, targetY: Int, movableObject: MovableObject?): Float
    }

    private data class Node(val x: Int, val y: Int) : Comparable<Node> {
        enum class State {
            UNEVALUATED,
            OPEN,
            CLOSED
        }

        var cost = 0.0f
        var parent: Node? = null
            set(value) {
                field = value
                depth = (value?.depth ?: -1) + 1
            }

        var heuristic = 0.0f
        var depth = 0
        var state = State.UNEVALUATED

        fun reset() {
            state = State.UNEVALUATED
            cost = 0.0f
            depth = 0
        }

        override fun compareTo(other: Node): Int {
            val f = heuristic + cost
            val of = other.heuristic + other.cost

            return when {
                f < of -> -1
                f > 0f -> 1
                else -> 0
            }
        }
    }

    private inner class Nodes {
        private val array = Array2D<Node>(map.numTilesX, map.numTilesY)
        private val open = PriorityList<Node>()
        private val closed = arrayListOf<Node>()

        init {
            repeat(map.numTilesX) { x ->
                repeat(map.numTilesY) { y ->
                    array[x, y] = Node(x, y)
                }
            }
        }

        fun reset() {
            open.clear()
            closed.clear()

            repeat(map.numTilesX) { x ->
                repeat(map.numTilesY) { y ->
                    this[x, y]?.reset()
                }
            }
        }

        operator fun get(x: Int, y: Int) = array[x, y]

        fun setState(node: Node, state: Node.State) {
            node.state = state
        }

        fun setOpen(x: Int, y: Int) = this[x, y]?.let { setOpen(it) }

        fun setOpen(node: Node) {
            setState(node, Node.State.OPEN)
            if (node.state == Node.State.CLOSED)
                closed -= node
            open += node
        }

        fun setClosed(node: Node) {
            setState(node, Node.State.CLOSED)
            if (node.state == Node.State.OPEN)
                open -= node
            closed += node
        }

        fun setUnevaluated(node: Node) {
            when (node.state) {
                Node.State.OPEN -> open -= node
                Node.State.CLOSED -> closed -= node
                else -> {}
            }
            node.state = Node.State.UNEVALUATED
        }

        fun setParent(x: Int, y: Int, parent: Node?) {
            array[x, y]?.parent = parent
        }

        fun getFirstOpen() = open.first()

        fun hasOpen() = open.isNotEmpty()
    }

    private val nodes by threadLocal { Nodes() }

    override fun findPath(movableObject: MovableObject?, startX: Int, startY: Int, targetX: Int, targetY: Int, maxSearchDistance: Int, allowDiagonalMovement: Boolean): Path? {
        fun isValidLocation(context: PathFindingContext, sx: Int, sy: Int, x: Int, y: Int): Boolean {
            var invalid = (x < 0) || (y < 0) || (x >= map.numTilesX) || (y >= map.numTilesY)

            if (!invalid && (sx != x || sy != y))
                invalid = map.isBlocked(x, y, sx, sy, context)

            return !invalid
        }

        val nodes = this.nodes
        val pathFindingContext = PathFindingContext(startX, startY, movableObject)

        if (map.isBlocked(targetX, targetY, targetX, targetY, pathFindingContext))
            return null

        nodes.reset()
        nodes.setOpen(startX, startY)
        nodes.setParent(targetX, targetY, null)

        var maxDepth = 0
        var current: Node? = null

        while ((maxDepth < maxSearchDistance) && nodes.hasOpen()) {
            var lx = startX
            var ly = startY

            if (current != null) {
                lx = current.x
                ly = current.y
            }

            current = nodes.getFirstOpen()
            pathFindingContext.searchDistance = current.depth

            if (current == nodes[targetX, targetY])
                if (isValidLocation(pathFindingContext, lx, ly, targetX, targetY))
                    break

            nodes.setClosed(current)

            for (x in -1..1)
                for (y in -1..1) {
                    if (y == 0 && x == 0)
                        continue

                    if (!allowDiagonalMovement && (x != 0 && y != 0))
                        continue

                    val xp = x + current.x
                    val yp = y + current.y

                    if (isValidLocation(pathFindingContext, current.x, current.y, xp, yp)) {
                        val nextStepCost = current.cost + map.getCost(xp, yp, current.x, current.y, pathFindingContext)
                        val neighbor = nodes[xp, yp] ?: continue

                        if (nextStepCost < neighbor.cost)
                            nodes.setUnevaluated(neighbor)

                        if (neighbor.state == Node.State.UNEVALUATED) {
                            neighbor.cost = nextStepCost
                            neighbor.heuristic = heuristic.getCost(map, xp, yp, targetX, targetY, movableObject)
                            neighbor.parent = current
                            maxDepth = max(maxDepth, neighbor.depth)
                            nodes.setOpen(neighbor)
                        }
                    }
                }
        }

        if (nodes[targetX, targetY]?.parent == null)
            return null

        val path = Path()
        var target = requireNotNull(nodes[targetX, targetY])

        while (target != nodes[startX, startY]) {
            path.prependStep(target.x, target.y)
            target = requireNotNull(target.parent)
        }

        path.prependStep(startX, startY)

        return path
    }
}