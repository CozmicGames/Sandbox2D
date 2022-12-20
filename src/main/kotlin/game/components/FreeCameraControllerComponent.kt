package game.components

import com.cozmicgames.utils.Properties
import com.cozmicgames.utils.Updateable
import engine.Game
import engine.scene.Component
import engine.scene.components.TransformComponent
import kotlin.math.max

class FreeCameraControllerComponent : Component(), Updateable {
    var moveActionName = "freecamera_move"
    var moveXActionName = "freecamera_move_x"
    var moveYActionName = "freecamera_move_y"
    var zoomActionName = "freecamera_zoom"

    var moveSpeed = 60.0f
    var zoomSpeed = 8.0f

    var isEnabled = true

    override fun update(delta: Float) {
        if (!isEnabled)
            return

        val cameraComponent = gameObject.getComponent<CameraComponent>() ?: return
        val transformComponent = gameObject.getComponent<TransformComponent>() ?: return

        var moveX = 0.0f
        var moveY = 0.0f

        if (Game.controls.find(moveActionName)?.state == true) {
            Game.controls.find(moveXActionName)?.currentValue?.let { moveX = it * moveSpeed * delta }
            Game.controls.find(moveYActionName)?.currentValue?.let { moveY = it * moveSpeed * delta }
        }

        val zoom = (Game.controls.find(zoomActionName)?.currentValue ?: 0.0f) * zoomSpeed * delta

        moveX *= cameraComponent.zoom
        moveY *= cameraComponent.zoom

        transformComponent.transform.x -= moveX
        transformComponent.transform.y -= moveY
        cameraComponent.zoom -= zoom
        cameraComponent.zoom = max(cameraComponent.zoom, 0.001f)
    }

    override fun read(properties: Properties) {
        properties.getString("moveActionName")?.let { moveActionName = it }
        properties.getString("moveXActionName")?.let { moveXActionName = it }
        properties.getString("moveYActionName")?.let { moveYActionName = it }
        properties.getString("zoomActionName")?.let { zoomActionName = it }
        properties.getFloat("moveSpeed")?.let { moveSpeed = it }
        properties.getFloat("zoomSpeed")?.let { zoomSpeed = it }
        properties.getBoolean("isEnabled")?.let { isEnabled = it }
    }

    override fun write(properties: Properties) {
        properties.setString("moveActionName", moveActionName)
        properties.setString("moveXActionName", moveXActionName)
        properties.setString("moveYActionName", moveYActionName)
        properties.setString("zoomActionName", zoomActionName)
        properties.setFloat("moveSpeed", moveSpeed)
        properties.setFloat("zoomSpeed", zoomSpeed)
        properties.setBoolean("isEnabled", isEnabled)
    }
}