package game.states

import com.cozmicgames.Kore
import com.cozmicgames.graphics
import com.cozmicgames.utils.Color
import engine.Game
import engine.GameState
import engine.graphics.ui.GUI
import engine.graphics.ui.widgets.textButton

class MainMenuState : GameState {
    private lateinit var gui: GUI

    override fun onCreate() {
        gui = GUI()
    }

    override fun onFrame(delta: Float): GameState {
        Kore.graphics.clear(Color.LIME)

        var returnState = this as GameState

        gui.begin()
        gui.textButton("Test level") {
            returnState = LevelGameState()
        }
        gui.textButton("Edit level") {
            returnState = LevelEditorGameState()
        }
        gui.textButton("Zip") {
            Game.assets.createZipArchive()
        }
        gui.end()

        return returnState
    }

    override fun onDestroy() {
        gui.dispose()
    }
}