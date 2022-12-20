package engine.graphics.ui

import engine.graphics.ui.widgets.label

open class DragDropData<T : Any>(val payload: T, val onDrawPayload: GUI.() -> Unit) {
    internal var isRendered = false

    fun drawPayload(gui: GUI) {
        if (isRendered)
            return

        gui.topLayer {
            gui.layerUp {
                gui.transient(true, false) {
                    gui.setLastElement(gui.absolute(gui.touchPosition))
                    onDrawPayload(gui)
                }
            }
        }

        isRendered = true
    }
}

class StringDragDropData(payload: String) : DragDropData<String>(payload, { label(payload, skin.hoverColor) })
