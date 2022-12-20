package game

import com.cozmicgames.input.GamepadButtons
import com.cozmicgames.input.Keys
import engine.Game

object GameControls {
    val openMenuFromLevel = Game.controls.add("open_menu_from_level").also {
        it.addKey(Keys.KEY_ESCAPE)
        it.addGamepadButton(GamepadButtons.START)
    }
}