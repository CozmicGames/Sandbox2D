package engine.utils

import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.collections.Pool

open class CommandList<T : Any>(private val pool: Pool<T>) : Disposable {
    constructor(supplier: () -> T, reset: (T) -> Unit = {}) : this(Pool(supplier = supplier, reset = reset))

    private val commands = arrayListOf<T>()
    private val processingCommands = arrayListOf<T>()

    fun get() = pool.obtain()

    fun add(command: T) {
        commands += command
    }

    fun process(block: (T) -> Unit) {
        processingCommands.clear()
        processingCommands.addAll(commands)
        commands.clear()
        processingCommands.forEach {
            block(it)
            pool.free(it)
        }
    }

    override fun dispose() {
        pool.dispose()
    }
}

operator fun <T : Any> CommandList<T>.plusAssign(command: T) = add(command)