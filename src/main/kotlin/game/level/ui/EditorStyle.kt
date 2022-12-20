package game.level.ui

import com.cozmicgames.Kore
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.injector
import engine.Game

class EditorStyle {
    private val popupBorderColor = Color(0x7D7D73FF)
    private val popupDropShadowColor = Color(0x080A0FAA)
    private val popupTitleBackgroundColor = Color(0x424444FF)
    private val popupContentBackgroundColor = Color(0x575B5BFF)
    private val popupBorderSize = 2.5f

    var toolImageSize = 40.0f
    val panelContentBackgroundColor = Color(0x575B5BFF)
    val panelTitleBackgroundColor = Color(0x424444FF)

    val importPopupBorderColor = popupBorderColor
    val importPopupDropShadowColor = popupDropShadowColor
    val importPopupTitleBackgroundColor = popupTitleBackgroundColor
    val importPopupContentBackgroundColor = popupContentBackgroundColor
    val importPopupBorderSize = popupBorderSize

    val createFilePopupBorderColor = popupBorderColor
    val createFilePopupDropShadowColor = popupDropShadowColor
    val createFilePopupTitleBackgroundColor = popupTitleBackgroundColor
    val createFilePopupContentBackgroundColor = popupContentBackgroundColor
    val createFilePopupBorderSize = popupBorderSize

    val materialEditorPopupBorderColor = popupBorderColor
    val materialEditorPopupDropShadowColor = popupDropShadowColor
    val materialEditorPopupTitleBackgroundColor = popupTitleBackgroundColor
    val materialEditorPopupContentBackgroundColor = popupContentBackgroundColor
    val materialEditorPopupBorderSize = popupBorderSize

    val ruleGeneratorPopupBorderColor = popupBorderColor
    val ruleGeneratorPopupDropShadowColor = popupDropShadowColor
    val ruleGeneratorPopupTitleBackgroundColor = popupTitleBackgroundColor
    val ruleGeneratorPopupContentBackgroundColor = popupContentBackgroundColor
    val ruleGeneratorPopupBorderSize = popupBorderSize

    val ruleEditorCellBackgroundColor = Color(0x8E8F87FF.toInt())
    val ruleEditorCellBorderColor = Color(0x575B5BFF)
    val ruleEditorCellBorderSize = 5.5f




    val layerEditorBorderColor = Color(0x7D7D73FF)




    var assetSelectionPanelWidth = 1.0f
    var assetSelectorHeight = 0.3f
    var assetElementWidth = 50.0f
    var assetElementMinPadding = 6.0f
    var imageImportPreviewSize = 0.3f
    var materialEditorPreviewSize = 0.2f
}

val Game.editorStyle by Kore.context.injector { EditorStyle() }
