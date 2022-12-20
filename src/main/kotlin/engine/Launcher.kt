package engine

import com.cozmicgames.*

private const val CONFIG_FILE = "config.txt"

fun main() {
    val configuration = Configuration()

    if (!configuration.readFromFile(CONFIG_FILE))
        configuration.icons = arrayOf("internal/icons/icon.png")

    configuration.title = "GameOff 2022"

    Kore.start(Game, configuration) { DesktopPlatform() }

    configuration.writeToFile(CONFIG_FILE)
}
