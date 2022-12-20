package engine.plugins

import com.cozmicgames.Kore
import com.cozmicgames.files.FileHandle
import com.cozmicgames.files.Files
import com.cozmicgames.log
import com.cozmicgames.utils.Properties
import java.io.File
import java.net.URLClassLoader
import java.util.zip.ZipFile
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions

class PluginManager {
    private val plugins = arrayListOf<Plugin>()

    inline fun <reified T : PluginEvent> dispatchEvent(event: T) = dispatchEvent(event, T::class)

    fun <T : PluginEvent> dispatchEvent(event: T, type: KClass<T>) {
        plugins.forEach {
            it.hooks[type]?.let {
                (it as? Plugin.EventHook<T>)?.onEvent(event)
            }
        }
    }

    fun load(file: FileHandle) {
        if (file.type == Files.Type.ZIP || file.type == Files.Type.ASSET) {
            Kore.log.error(this::class, "Cannot load plugin from asset or zip file")
            return
        }

        val javaFile = File(file.fullPath)
        val jarFile = ZipFile(javaFile)
        lateinit var info: PluginInfo

        for (entry in jarFile.entries()) {
            if (entry.name == "plugin.properties ") {
                val properties = Properties()
                val text = jarFile.getInputStream(entry).bufferedReader().readText()
                properties.read(text)

                val pluginName = properties.getString("name")

                if (pluginName.isNullOrEmpty()) {
                    Kore.log.error(this::class, "Plugin name is empty")
                    return
                }

                val pluginVersion = properties.getString("version")

                if (pluginVersion.isNullOrEmpty()) {
                    Kore.log.error(this::class, "Plugin version is empty")
                    return
                }

                val pluginMainClass = properties.getString("mainClass")

                if (pluginMainClass.isNullOrEmpty()) {
                    Kore.log.error(this::class, "Plugin main class is empty")
                    return
                }

                info = PluginInfo(pluginName, pluginVersion, pluginMainClass)
                break
            }
        }

        val loader = URLClassLoader.newInstance(arrayOf(javaFile.toURI().toURL()), javaClass.classLoader)
        val mainClass = Class.forName(info.mainClass, true, loader).kotlin

        val hooks = hashMapOf<KClass<*>, Plugin.EventHook<*>>()

        mainClass.functions.forEach {
            val annotation = it.findAnnotation<SubscribeEvent>()
            if (it.visibility == KVisibility.PUBLIC && annotation != null)
                hooks[annotation.eventType] = Plugin.EventHook(it::call)
        }

        val plugin = Plugin(info, hooks)
        plugins += plugin
    }
}