package game.components

import com.cozmicgames.Kore
import com.cozmicgames.ResizeListener
import com.cozmicgames.graphics
import com.cozmicgames.utils.Properties
import com.cozmicgames.utils.Updateable
import com.cozmicgames.utils.maths.OrthographicCamera
import engine.scene.Component
import engine.scene.components.TransformComponent

class CameraComponent : Component(), Updateable {
    var isMainCamera = true
        set(value) {
            if (value) {
                gameObject.scene.gameObjects.forEach {
                    if (it != gameObject)
                        it.getComponent<CameraComponent>()?.isMainCamera = false
                }
            }

            field = value
        }

    var viewportWidth = Kore.graphics.width

    var viewportHeight = Kore.graphics.height

    var zoom = 1.0f

    var listenForResize = true

    val excludedLayers = hashSetOf<Int>()

    val camera = OrthographicCamera(viewportWidth, viewportHeight)

    private val resizeListener: ResizeListener = { width, height ->
        if (listenForResize) {
            viewportWidth = width
            viewportHeight = height
        }
    }

    override fun update(delta: Float) {
        camera.width = viewportWidth
        camera.height = viewportHeight
        camera.zoom = zoom

        gameObject.getComponent<TransformComponent>()?.let {
            camera.position.set(it.transform.x, it.transform.y, 0.0f)
        }

        camera.update()
    }

    override fun onAdded() {
        Kore.addResizeListener(resizeListener)
    }

    override fun onRemoved() {
        Kore.removeResizeListener(resizeListener)
    }

    override fun read(properties: Properties) {
        viewportWidth = properties.getInt("viewportWidth") ?: Kore.graphics.width
        viewportHeight = properties.getInt("viewportHeight") ?: Kore.graphics.height
        listenForResize = properties.getBoolean("listenForResize") ?: true
        zoom = properties.getFloat("zoom") ?: 1.0f

        properties.getIntArray("excludedLayers")?.let {
            excludedLayers.clear()
            excludedLayers += it
        }
    }

    override fun write(properties: Properties) {
        properties.setInt("viewportWidth", viewportWidth)
        properties.setInt("viewportHeight", viewportHeight)
        properties.setBoolean("listenForResize", listenForResize)
        properties.setFloat("zoom", zoom)
        properties.setIntArray("excludedLayers", excludedLayers.toTypedArray())
    }
}
