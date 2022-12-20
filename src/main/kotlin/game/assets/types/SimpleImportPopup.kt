package game.assets.types

import com.cozmicgames.Kore
import com.cozmicgames.files
import com.cozmicgames.files.FileHandle
import com.cozmicgames.files.nameWithExtension
import com.cozmicgames.utils.extensions.nameWithExtension
import engine.Game
import engine.graphics.ui.GUI
import engine.graphics.ui.TextData
import engine.graphics.ui.widgets.label
import engine.graphics.ui.widgets.textField
import game.assets.AssetType
import engine.assets.MetaFile

abstract class SimpleImportPopup(type: AssetType<*>, titleString: String) : ImportPopup(type, titleString) {
    private lateinit var file: String

    private val nameTextData = TextData {
        onImport()
        closePopup()
    }

    protected abstract fun onImport(file: FileHandle, name: String)

    override fun reset(file: String) {
        this.file = file
        nameTextData.setText(file.nameWithExtension)
    }

    override fun drawContent(gui: GUI, width: Float, height: Float) {
        gui.currentTextData = nameTextData

        gui.sameLine {
            gui.group {
                gui.label("Original filename", null)
                gui.label("Import filename", null)
            }
            gui.group {
                val originalWidth = gui.label(file, null).width
                gui.textField(nameTextData, originalWidth)
            }
        }
    }

    override fun onImport() {
        val assetFile = Game.assets.toAssetFileHandle(nameTextData.text)

        if (file != nameTextData.text) {
            if (assetFile.exists)
                assetFile.delete()

            Kore.files.absolute(file).copyTo(assetFile)

            val metaFile = MetaFile()
            metaFile.name = nameTextData.text
            metaFile.write(assetFile.sibling("${assetFile.nameWithExtension}.meta"))
        }

        onImport(assetFile, nameTextData.text)
    }
}
