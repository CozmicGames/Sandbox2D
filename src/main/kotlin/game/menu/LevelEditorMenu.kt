package game.menu

import com.cozmicgames.Kore
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.Disposable
import engine.graphics.ui.GUI
import engine.graphics.ui.widgets.textButton

class LevelEditorMenu : Disposable {
    sealed interface ReturnState {
        object None : ReturnState

        object LevelEditor : ReturnState

        object MainMenu : ReturnState
    }

    private val gui = GUI()

    fun onFrame(): ReturnState {
        var returnState: ReturnState = ReturnState.None

        gui.begin()

        gui.group(Color(0xFFF5CCFF.toInt())) {
            gui.textButton("Resume") {
                returnState = ReturnState.LevelEditor
            }
            gui.textButton("To Menu") {
                println("To menu") //TODO
                returnState = ReturnState.MainMenu
            }
            gui.textButton("Settings") {
                println("Settings") //TODO: Use popup for this?
            }
            gui.textButton("Close Game") {
                Kore.stop()
            }
        }
        gui.end()

        return returnState
    }

    override fun dispose() {
        gui.dispose()
    }
}