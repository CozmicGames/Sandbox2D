package engine.utils

import com.cozmicgames.utils.maths.Camera
import com.cozmicgames.utils.maths.Vector2

class CameraFollower(var camera: Camera, var follow: Vector2, var drag: Float = 0.0f) {
    fun update() {
        val dx = follow.x - camera.position.x
        val dy = follow.y - camera.position.y
        camera.position.x += dx * (1.0f - drag)
        camera.position.y += dy * (1.0f - drag)
        camera.update()
    }
}