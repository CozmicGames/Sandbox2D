package engine.graphics.ui.style

import engine.graphics.ui.GUICommandList

interface GUIDrawable {
    val paddingLeft get() = 0.0f
    val paddingRight get() = 0.0f
    val paddingTop get() = 0.0f
    val paddingBottom get() = 0.0f

    fun draw(commands: GUICommandList, x: Float, y: Float, width: Float, height: Float, state: Int)
}