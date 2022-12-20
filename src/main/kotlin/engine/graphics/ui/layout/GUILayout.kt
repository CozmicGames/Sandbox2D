package engine.graphics.ui.layout

import com.cozmicgames.utils.Disposable
import engine.graphics.ui.GUI
import engine.graphics.ui.GUISkin

class GUILayout : Disposable {
    private val styles = hashMapOf<String, GUISkin>()
    private val guis = hashMapOf<String, GUI>()
    private val regions = hashMapOf<String, GUIRegion>()

    init {
        addStyle("default") {}
    }

    fun addStyle(name: String, block: GUISkin.() -> Unit) {
        styles.getOrPut(name) { GUISkin() }.apply(block)
    }

    fun addRegion(name: String, style: String = "default", block: GUIRegion.() -> Unit) {
        val gui = getGUI(style) ?: requireNotNull(getGUI("default"))
        val region = regions.getOrPut(name) { GUIRegion(gui) }
        region.apply(block)
    }

    fun getGUI(style: String): GUI? {
        if (style !in styles)
            return null

        return guis.getOrPut(style) {
            GUI(requireNotNull(styles[style]))
        }
    }

    fun getRegion(name: String): GUIRegion? {
        return regions[name]
    }

    fun render(delta: Float) {
        regions.values.forEach {
            it.animator.update(delta)
            it.render()
        }
    }

    override fun dispose() {
        guis.forEach { it.value.dispose() }
    }
}