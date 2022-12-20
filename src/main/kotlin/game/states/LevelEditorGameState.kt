package game.states

import com.cozmicgames.Kore
import com.cozmicgames.graphics
import com.cozmicgames.utils.Color
import engine.Game
import engine.GameState
import engine.graphics.asRegion
import engine.graphics.rendergraph.RenderFunction
import engine.graphics.rendergraph.RenderGraph
import engine.graphics.rendergraph.colorRenderTargetDependency
import engine.graphics.rendergraph.functions.BlurHorizontalRenderFunction
import engine.graphics.rendergraph.functions.BlurVerticalRenderFunction
import engine.graphics.rendergraph.onRender
import engine.graphics.rendergraph.passes.ColorRenderPass
import engine.graphics.rendergraph.present.SimplePresentFunction
import engine.scene.Scene
import engine.scene.components.TransformComponent
import engine.scene.processors.DrawableRenderProcessor
import engine.scene.processors.ParticleRenderProcessor
import engine.scene.processors.SpriteRenderProcessor
import game.components.FreeCameraControllerComponent
import game.GameControls
import game.components.CameraComponent
import game.level.ui.LevelEditor
import game.menu.LevelEditorMenu

class LevelEditorGameState : GameState {
    private companion object {
        const val LEVEL_EDITOR_PASS_NAME = "levelEditor"
        const val LEVEL_EDITOR_BLUR_PREPASS_NAME = "levelEditorBlurPre"
        const val LEVEL_EDITOR_BLUR_PASS_NAME = "levelEditorBlur"
        const val MENU_PASS_NAME = "menuFromLevelEditor"
    }

    private enum class PresentSource(val passName: String) {
        LEVEL_EDITOR(LEVEL_EDITOR_PASS_NAME),
        MENU(MENU_PASS_NAME),
        MAIN_MENU(MENU_PASS_NAME)
    }

    private val scene = Scene()
    private val levelEditor = LevelEditor(scene)
    private val levelEditorMenu = LevelEditorMenu()
    private var isMenuOpen = false
    private val renderGraph = RenderGraph(SimplePresentFunction(LEVEL_EDITOR_PASS_NAME, 0))
    private var newPresentSource: PresentSource? = null
    private val resizeListener = renderGraph::resize

    override fun onCreate() {
        scene.addGameObject {
            addComponent<TransformComponent> { }
            addComponent<CameraComponent> {
                isMainCamera = true
            }
            addComponent<FreeCameraControllerComponent> { }
        }

        scene.addSceneProcessor(SpriteRenderProcessor())
        scene.addSceneProcessor(DrawableRenderProcessor())
        scene.addSceneProcessor(ParticleRenderProcessor())

        renderGraph.onRender(LEVEL_EDITOR_PASS_NAME, ColorRenderPass(), object : RenderFunction() {
            override fun render(delta: Float) {
                Kore.graphics.clear(Color(0x726D8AFF))

                val returnState = levelEditor.onFrame(delta)

                if (returnState is LevelEditor.ReturnState.Menu)
                    setPresentSource(PresentSource.MENU)

                if (returnState is LevelEditor.ReturnState.MainMenu) {
                    setPresentSource(PresentSource.MAIN_MENU)
                }
            }
        })

        renderGraph.onRender(LEVEL_EDITOR_BLUR_PREPASS_NAME, ColorRenderPass(), BlurHorizontalRenderFunction(LEVEL_EDITOR_PASS_NAME, 0))
        renderGraph.onRender(LEVEL_EDITOR_BLUR_PASS_NAME, ColorRenderPass(), BlurVerticalRenderFunction(LEVEL_EDITOR_BLUR_PREPASS_NAME, 0))

        renderGraph.onRender(MENU_PASS_NAME, ColorRenderPass(), object : RenderFunction() {
            private val colorInput = colorRenderTargetDependency(LEVEL_EDITOR_BLUR_PASS_NAME, 0)

            override fun render(delta: Float) {
                Kore.graphics.clear(Color.CLEAR)

                Game.graphics2d.render {
                    it.draw(colorInput.texture.asRegion(), 0.0f, 0.0f, pass.width.toFloat(), pass.height.toFloat())
                }

                val returnState = levelEditorMenu.onFrame()

                if (returnState is LevelEditorMenu.ReturnState.LevelEditor)
                    setPresentSource(PresentSource.MENU)

                if (returnState is LevelEditorMenu.ReturnState.MainMenu)
                    setPresentSource(PresentSource.MAIN_MENU)
            }
        })

        Kore.addResizeListener(resizeListener)
    }

    private fun setPresentSource(presentSource: PresentSource) {
        newPresentSource = presentSource
    }

    override fun onFrame(delta: Float): GameState {
        newPresentSource?.let {
            if (it == PresentSource.LEVEL_EDITOR)
                levelEditor.enableInteraction()
            else
                levelEditor.disableInteraction()

            if (it == PresentSource.MAIN_MENU)
                return MainMenuState()

            isMenuOpen = it == PresentSource.MENU

            renderGraph.presentRenderFunction = SimplePresentFunction(it.passName, 0)
            newPresentSource = null
        }

        renderGraph.render(delta)

        if (GameControls.openMenuFromLevel.isTriggered)
            setPresentSource(if (isMenuOpen) PresentSource.LEVEL_EDITOR else PresentSource.MENU)

        return this
    }

    override fun onDestroy() {
        scene.dispose()
        renderGraph.dispose()
        levelEditor.dispose()
        levelEditorMenu.dispose()
        Kore.removeResizeListener(resizeListener)
    }
}