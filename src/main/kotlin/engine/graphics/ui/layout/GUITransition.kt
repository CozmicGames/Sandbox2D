package engine.graphics.ui.layout

import com.cozmicgames.utils.maths.Easing
import com.cozmicgames.utils.maths.lerp

class GUITransition(val animator: GUIAnimator, val from: GUIAnimator.State, val to: GUIAnimator.State, val duration: Float, val easing: Easing = Easing.LINEAR) {
    private var progress = 0.0f

    fun update(delta: Float): Float {
        progress += delta
        val t = easing(progress / duration)
        animator.x = lerp(from.x, to.x, t)
        animator.y = lerp(from.y, to.y, t)
        animator.width = lerp(from.width, to.width, t)
        animator.height = lerp(from.height, to.height, t)
        return progress - duration
    }
}