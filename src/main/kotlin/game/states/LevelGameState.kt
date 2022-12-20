package game.states

import com.cozmicgames.Kore
import com.cozmicgames.files
import com.cozmicgames.files.readToString
import com.cozmicgames.files.writeString
import com.cozmicgames.graphics
import com.cozmicgames.input
import com.cozmicgames.input.Keys
import com.cozmicgames.input.MouseButtons
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.Properties
import engine.Game
import engine.GameState
import engine.graphics.ui.GUI
import engine.graphics.ui.widgets.textButton
import engine.scene.Scene
import engine.scene.components.NameComponent
import engine.scene.components.SpriteComponent
import engine.scene.components.TransformComponent
import engine.scene.components.findGameObjectByName
import engine.scene.processors.DrawableRenderProcessor
import engine.scene.processors.ParticleRenderProcessor
import engine.scene.processors.SpriteRenderProcessor
import game.components.FreeCameraControllerComponent
import game.GameControls
import game.components.CameraComponent
import game.components.GridComponent
import game.components.PlayerInputComponent

class LevelGameState : GameState {
    private val scene = Scene()
    private val ui = GUI()
    private var isMenuOpen = false

    override fun onCreate() {
        scene.addSceneProcessor(SpriteRenderProcessor())
        scene.addSceneProcessor(DrawableRenderProcessor())
        scene.addSceneProcessor(ParticleRenderProcessor())

        scene.addGameObject {
            addComponent<TransformComponent> {  }
            addComponent<CameraComponent> {
                isMainCamera = true
            }
            addComponent<FreeCameraControllerComponent> { }
        }

        //TODO: Remove, this is just for testing

        Game.controls.add("freecamera_move").also {
            it.addMouseButton(MouseButtons.MIDDLE)
        }

        Game.controls.add("freecamera_move_x").also {
            it.setDeltaX()
        }

        Game.controls.add("freecamera_move_y").also {
            it.setDeltaY()
        }

        Game.controls.add("freecamera_zoom").also {
            it.setScrollY()
        }

        val gridObject = scene.addGameObject {
            addComponent<TransformComponent> {}
            addComponent<GridComponent> {
                tileSet = "assets/tilesets/test.tileset"
            }
        }

        scene.addGameObject {
            addComponent<NameComponent> {
                name = "player"
            }

            addComponent<TransformComponent> {
                transform.x = 100.0f
                transform.y = 100.0f
                transform.scaleX = 100.0f
                transform.scaleY = 100.0f
            }

            addComponent<SpriteComponent> {
                layer = 1
                material = "assets/materials/template.material"
            }
        }
    }

    override fun onFrame(delta: Float): GameState {
        Kore.graphics.clear(Color.LIGHT_GRAY)

        if (Kore.input.isKeyJustDown(Keys.KEY_S)) {
            val properties = Properties()
            scene.write(properties)

            if (Kore.input.isKeyDown(Keys.KEY_SHIFT))
                Kore.files.local("scene.txt").writeString(properties.write(false), false)
            else
                Kore.files.local("scene.txt").writeString(properties.write(), false)
        }

        if (Kore.input.isKeyJustDown(Keys.KEY_C))
            scene.clearGameObjects()

        if (Kore.input.isKeyJustDown(Keys.KEY_L)) {
            val properties = Properties()
            properties.read(Kore.files.local("scene.txt").readToString())
            scene.read(properties)
        }

        scene.update(delta)

        for (gameObject in scene.activeGameObjects) {
            val cameraComponent = gameObject.getComponent<CameraComponent>() ?: continue

            if (cameraComponent.isMainCamera) {
                Game.renderer.render(cameraComponent.camera) { it !in cameraComponent.excludedLayers }
                break
            }
        }

        Game.renderer.clear()

        if (GameControls.openMenuFromLevel.isTriggered)
            isMenuOpen = !isMenuOpen

        scene.findGameObjectByName("player")?.getComponent<PlayerInputComponent>()?.isInMenu = isMenuOpen

        if (isMenuOpen) {
            ui.begin()
            ui.group(Color(0xFFF5CCFF.toInt())) {
                ui.textButton("Resume") {
                    isMenuOpen = false
                }
                ui.textButton("To Menu") {
                    println("To menu") //TODO
                }
                ui.textButton("Settings") {
                    println("Settings") //TODO
                }
                ui.textButton("Close Game") {
                    Kore.stop()
                }
            }
            ui.end()
        }

        return this
    }

    override fun onDestroy() {
        scene.dispose()
        ui.dispose()
    }
}