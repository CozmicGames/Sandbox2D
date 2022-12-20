package engine.utils

import com.cozmicgames.utils.Time
import com.cozmicgames.utils.TimeUnit
import com.cozmicgames.utils.durationOf

fun <R> sleepUnusedTime(time: Double, block: () -> R): R {
    var result: R? = null
    val duration = durationOf {
        result = block()
    }
    val sleepTime = time - duration
    if (sleepTime > 0.0)
        Thread.sleep(Time.convert(sleepTime, TimeUnit.SECONDS, TimeUnit.MILLISECONDS).toLong())
    return result!!
}
