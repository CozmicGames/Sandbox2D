package engine.plugins

import com.cozmicgames.utils.events.EventListener
import kotlin.reflect.KClass

class Plugin(val info: PluginInfo, val hooks: Map<KClass<*>, EventHook<*>>) {
    class EventHook<T : PluginEvent>(val function: (T) -> Unit) : EventListener<T> {
        override fun onEvent(event: T) {
            function(event)
        }
    }
}