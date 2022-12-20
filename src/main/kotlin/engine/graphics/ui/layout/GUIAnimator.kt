package engine.graphics.ui.layout

import com.cozmicgames.utils.maths.Easing

class GUIAnimator {
    open class State(var x: Float = 0.0f, var y: Float = 0.0f, var width: Float = 1.0f, var height: Float = 1.0f)

    var x = 0.0f
        internal set

    var y = 0.0f
        internal set

    var width = 1.0f
        internal set

    var height = 1.0f
        internal set

    private val transitions = arrayListOf<GUITransition>()

    fun transitionTo(state: State, duration: Float, easing: Easing = Easing.LINEAR) {
        val startState = transitions.lastOrNull()?.to ?: State(x, y, width, height)
        transitions += GUITransition(this, startState, state, duration, easing)
    }

    fun update(delta: Float) {
        var transition = transitions.firstOrNull() ?: return
        var remainingDelta = transition.update(delta)

        while (remainingDelta > 0.0f) {
            transitions.removeFirst()
            transition = transitions.firstOrNull() ?: return
            remainingDelta = transition.update(delta)
        }
    }

    fun reset() {
        transitions.clear()
        x = 0.0f
        y = 0.0f
        width = 1.0f
        height = 1.0f
    }
}