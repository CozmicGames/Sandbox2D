package engine.utils.action

import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.Time
import java.util.concurrent.Executors

class ActionContext : Disposable {
    open class Runner(private var action: Action) {
        fun update(delta: Float): Boolean {
            action.update(delta)
            if (action.isDone)
                action = action.next ?: return false
            return true
        }
    }

    private inner class AsyncRunner(action: Action) : Runner(action) {
        init {
            executor.submit {
                var previousTime = Time.current
                while (!isDisposed) {
                    val currentTime = Time.current
                    val delta = (currentTime - previousTime).toFloat()
                    previousTime = currentTime

                    if (!update(delta))
                        break
                }
            }
        }
    }

    private val executor = Executors.newCachedThreadPool()
    private val runners = arrayListOf<Runner>()
    private val processingRunners = arrayListOf<Runner>()
    private var isDisposed = false

    fun update(delta: Float) {
        processingRunners.clear()
        processingRunners.addAll(runners)

        for (runner in processingRunners) {
            if (runner is AsyncRunner)
                continue

            if (!runner.update(delta))
                runners -= runner
        }
    }

    fun start(action: Action) {
        runners += Runner(action.startAction)
    }

    fun startAsync(action: Action) {
        runners += AsyncRunner(action.startAction)
    }

    override fun dispose() {
        isDisposed = true
        executor.shutdown()
        runners.clear()
    }
}

fun start(action: Action, context: ActionContext) = context.start(action)

fun startAsync(action: Action, context: ActionContext) = context.startAsync(action)