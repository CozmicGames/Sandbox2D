package engine

interface GameState {
    fun onCreate() {}
    fun onFrame(delta: Float): GameState
    fun onDestroy() {}
}