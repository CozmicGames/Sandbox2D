package engine.graphics.ui

class GUIElement {
    var x = 0.0f
    var y = 0.0f
    var width = 0.0f
    var height = 0.0f

    var getNextX: () -> Float = { 0.0f }
    var getNextY: () -> Float = { 0.0f }

    val nextX get() = getNextX()
    val nextY get() = getNextY()

    operator fun component1() = nextX
    operator fun component2() = nextY
}
