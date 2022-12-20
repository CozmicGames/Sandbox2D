package engine.utils.action

import com.cozmicgames.utils.maths.Easing
import engine.utils.sleepUnusedTime

abstract class Action {
    internal var previous: Action? = null

    internal var next: Action? = null
        private set

    var isDone = false
        private set

    internal val startAction: Action
        get() {
            val previous = previous ?: return this
            return previous.startAction
        }

    abstract fun update(delta: Float)

    fun setDone() {
        isDone = true
    }

    fun cancel() {
        setDone()
        next = null
    }

    infix fun then(action: Action): Action {
        action.previous = this
        next = action
        return action
    }
}

inline infix fun Action.then(noinline event: Action.() -> Unit) = this then action(event)

fun action(f: Action.() -> Unit) = object : Action() {
    override fun update(delta: Float) {
        f(this)
        setDone()
    }
}

fun repeat(count: Int, f: Action.(Int) -> Unit) = object : Action() {
    private var i = 0

    override fun update(delta: Float) {
        f(this, i)
        if (i++ >= count)
            setDone()
    }
}

fun delay(duration: Double) = object : Action() {
    private var time = 0.0

    override fun update(delta: Float) {
        time += delta
        if (time >= duration)
            setDone()
    }
}

fun tween(duration: Double, easing: Easing, f: Action.(Float) -> Unit) = object : Action() {
    private var time = 0.0
    private var firstUpdate = true

    override fun update(delta: Float) {
        if (!firstUpdate)
            time += delta
        else
            firstUpdate = false

        if (time >= duration) {
            setDone()
            f(this, easing(1.0f))
        } else
            f(this, easing((time / duration).toFloat()))
    }
}

fun waitFor(condition: () -> Boolean) = object : Action() {
    override fun update(delta: Float) {
        if (condition())
            setDone()
    }
}

fun onUpdate(frequency: Double, f: Action.(Float) -> Unit) = object : Action() {
    private var counter = 0.0

    override fun update(delta: Float) {
        counter += delta
        while (counter >= frequency) {
            f(this, frequency.toFloat())
            counter -= frequency.toFloat()
        }
    }
}

fun onUpdate(rate: Int, f: Action.(Float) -> Unit) = onUpdate(1.0 / rate, f)

fun onUpdateWithSleep(frequency: Double, f: Action.(Float) -> Unit) = onUpdate(frequency) {
    sleepUnusedTime(frequency) {
        f(this, it)
    }
}

fun onUpdateWithSleep(rate: Int, f: Action.(Float) -> Unit) = onUpdateWithSleep(1.0 / rate, f)

fun Action.start(context: ActionContext) = start(this, context)

fun Action.startAsync(context: ActionContext) = startAsync(this, context)
