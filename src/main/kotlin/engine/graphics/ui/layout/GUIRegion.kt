package engine.graphics.ui.layout

import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.maths.Vector2
import engine.graphics.ui.GUI
import engine.graphics.ui.widgets.panel

class GUIRegion(val gui: GUI, private val ownsGUI: Boolean = false) : Disposable {
    constructor() : this(GUI(), true)

    val constraints = GUIConstraints()

    val animator = GUIAnimator()

    val x get() = constraints.x.getValue(parent, this) + animator.x

    val y get() = constraints.y.getValue(parent, this) + animator.y

    val width get() = constraints.width.getValue(parent, this) * animator.width

    val height get() = constraints.height.getValue(parent, this) * animator.height

    var parent: GUIRegion? = null

    var layoutElements: (GUI) -> Unit = {}

    private val scrollAmount = Vector2()

    fun render() {
        gui.begin()
        gui.setLastElement(gui.absolute(x, y))
        gui.panel(width, height, scrollAmount, gui.skin.backgroundColor) {
            layoutElements(gui)
        }
        gui.end()
    }

    override fun dispose() {
        if (ownsGUI)
            gui.dispose()
    }
}