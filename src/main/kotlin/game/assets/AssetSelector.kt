package game.assets

import engine.Game
import engine.assets.getAssetNames
import engine.graphics.ui.*
import engine.graphics.ui.widgets.*
import engine.assets.managers.getTexture
import game.extensions.multilineListWithSameElementWidths
import game.level.ui.editorStyle

fun GUI.assetSelector(data: AssetSelectorData, width: Float, height: Float): GUIElement {
    val filteredAssetTypes = Game.assetTypes.types.filter {
        it.assetType in data.filter
    }

    return panel(width, height, data.elementsScroll, Game.editorStyle.panelContentBackgroundColor, Game.editorStyle.panelTitleBackgroundColor, {
        val assetTypeSelector = {
            sameLine {
                if (filteredAssetTypes.size == 1) {
                    val type = filteredAssetTypes.first()
                    imageLabel(type.name, Game.assets.getTexture(type.iconName))
                    data.currentAssetType = type.assetType
                } else
                    filteredAssetTypes.forEach {
                        selectableText(it.name, Game.assets.getTexture(it.iconName), data.currentAssetType == it.assetType) {
                            data.currentAssetType = it.assetType
                            data.elementsScroll.setZero()
                        }
                        spacing(skin.elementPadding)
                    }
            }
        }

        val filterText = {
            sameLine {
                image(Game.assets.getTexture("internal/images/search.png"), borderThickness = 0.0f)
                textField(data.filterTextData, skin.elementSize * 6.0f)
            }
        }

        val assetTypeSelectorWidth = getElementSize(assetTypeSelector).width
        val filterTextWidth = getElementSize(filterText).width

        scrollArea(maxWidth = width, scroll = data.assetTitleScroll) {
            sameLine {
                assetTypeSelector()
                val spacingAmount = width - assetTypeSelectorWidth - filterTextWidth
                if (spacingAmount > 0.0f)
                    spacing(spacingAmount)
                filterText()
            }
        }
    }) {
        val currentAssetType = data.currentAssetType

        if (currentAssetType != null) {
            val type = Game.assetTypes.findAssetType(currentAssetType)

            if (type != null) {
                val elements = Game.assets.getAssetNames(type.assetType).filter {
                    if (data.showInternalAssetElements || !it.startsWith("internal")) {
                        if (data.filterTextData.text.isNotBlank())
                            data.filterTextData.text in it
                        else
                            true
                    } else
                        false
                }.mapTo(arrayListOf()) {
                    {
                        draggable(type.createDragDropData(it)) {
                            group {
                                type.preview(this, Game.editorStyle.assetElementWidth, it, data.showEditIcons)
                                tooltip(label(it, backgroundColor = null, maxWidth = Game.editorStyle.assetElementWidth), it)
                            }
                        }
                    }
                }

                type.appendToAssetList(this, elements)

                multilineListWithSameElementWidths(width - skin.scrollbarSize, Game.editorStyle.assetElementWidth, Game.editorStyle.assetElementMinPadding) {
                    elements.removeFirstOrNull()
                }
            }
        }
    }
}
