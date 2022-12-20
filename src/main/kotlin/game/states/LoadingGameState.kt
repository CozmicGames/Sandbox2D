package game.states

import com.cozmicgames.Kore
import com.cozmicgames.files
import com.cozmicgames.files.FileHandle
import com.cozmicgames.files.extension
import com.cozmicgames.files.isDirectory
import com.cozmicgames.graphics
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.durationOf
import com.cozmicgames.utils.extensions.extension
import engine.Game
import engine.GameState
import engine.graphics.ui.GUI
import engine.graphics.ui.widgets.label
import game.Version
import engine.assets.managers.getTexture
import kotlin.math.min

class LoadingGameState : GameState {
    private class LoadingTask(val file: FileHandle) {
        fun load() {
            Game.assets.load(file)
        }
    }

    private val loadingTasks = arrayListOf<LoadingTask>()
    private var totalTasks = 0
    private var loadedTasks = 0

    private val ui = GUI()

    override fun onCreate() {

        val supportedAssetFormats = arrayListOf<String>()
        Game.assets.managers.forEach {
            supportedAssetFormats.addAll(it.supportedFormats)
        }

        fun searchDirectory(directoryFile: FileHandle) {
            directoryFile.list {
                val file = directoryFile.child(it)

                if (file.isDirectory)
                    searchDirectory(file)
                else if (it.extension in supportedAssetFormats)
                    loadingTasks += LoadingTask(file)
            }
        }

        Kore.files.local("assets.zip").openZip()?.list {
            if (it.isDirectory)
                searchDirectory(it)
            else if (it.extension in supportedAssetFormats)
                loadingTasks += LoadingTask(it)
        }

        searchDirectory(Kore.files.asset("assets"))
        searchDirectory(Kore.files.asset("internal"))
        searchDirectory(Kore.files.local("assets"))

        Game.assets.load(Kore.files.asset("internal/icons/icon.png"))

        totalTasks = loadingTasks.size
    }

    override fun onFrame(delta: Float): GameState {
        var usedTime = 0.0

        while (loadingTasks.isNotEmpty() && usedTime < 1.0f / delta) {
            usedTime += durationOf {
                val task = loadingTasks.removeFirst()
                task.load()
                loadedTasks++
            }
        }

        Kore.graphics.clear(Color.DARK_GRAY)

        val progressWidth = Kore.graphics.width * 0.75f
        val progressHeight = 24.0f

        val progressX = Kore.graphics.width * 0.5f
        val progressY = Kore.graphics.height * 0.25f

        val iconSize = min(Kore.graphics.width, Kore.graphics.height) * 0.5f

        val iconX = (Kore.graphics.width - iconSize) * 0.5f
        val iconY = (Kore.graphics.height - iconSize) * 0.66f

        val progress = loadedTasks.toFloat() / totalTasks

        Game.graphics2d.render {
            it.draw(Game.assets.getTexture("internal/icons/icon.png"), iconX, iconY, iconSize, iconSize)
            it.drawRect(progressX, progressY, progressWidth, progressHeight, Color.GRAY)
            it.drawRect(progressX, progressY, progressWidth * progress, progressHeight, Color.RED)
        }

        ui.begin()
        ui.label(Version.versionString, Color.CLEAR)
        ui.end()

        return if (loadedTasks == totalTasks) MainMenuState() else this
    }
}
