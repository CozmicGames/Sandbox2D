package engine.graphics.ui.layout

import com.cozmicgames.utils.maths.Easing

fun GUIRegion.slideOutLeft(duration: Float = 0.15f, easing: Easing = Easing.CUBIC_IN) {
    animator.transitionTo(GUIAnimator.State(x = -width), duration, easing)
}

fun GUIRegion.slideOutRight(duration: Float = 0.15f, easing: Easing = Easing.CUBIC_IN) {
    animator.transitionTo(GUIAnimator.State(x = width), duration, easing)
}

fun GUIRegion.slideOutBottom(duration: Float = 0.15f, easing: Easing = Easing.CUBIC_IN) {
    animator.transitionTo(GUIAnimator.State(y = height), duration, easing)
}

fun GUIRegion.slideOutTop(duration: Float = 0.15f, easing: Easing = Easing.CUBIC_IN) {
    animator.transitionTo(GUIAnimator.State(y = -height), duration, easing)
}

fun GUIRegion.resetToNormal(duration: Float = 0.15f, easing: Easing = Easing.CUBIC_IN) {
    animator.transitionTo(GUIAnimator.State(), duration, easing)
}


