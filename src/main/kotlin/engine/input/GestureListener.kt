package engine.input

interface GestureListener {
    fun onTap(x: Float, y: Float, count: Int) {}
    fun onLongPress(x: Float, y: Float) {}
}