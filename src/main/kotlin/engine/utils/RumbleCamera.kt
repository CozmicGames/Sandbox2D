package engine.utils

import com.cozmicgames.utils.Updateable
import com.cozmicgames.utils.maths.Camera
import com.cozmicgames.utils.maths.Matrix4x4
import com.cozmicgames.utils.maths.Vector3
import com.cozmicgames.utils.maths.randomFloat

class RumbleCamera(val camera: Camera) : Updateable, Camera() {
    private var rumbleTimeLeft = 0.0
    private var currentTime = 0.0f
    private var power = 0.0f
    private var currentPower = 0.0f

    private val rumbleTransform = Matrix4x4()

    override val direction by camera::direction
    override val up by camera::up
    override val projection by camera::projection

    val rumblePosition = Vector3()

    fun rumble(power: Float, duration: Double) {
        this.power = power
        rumbleTimeLeft = duration
        currentTime = 0.0f
    }

    override fun update(delta: Float) {
        if (currentTime <= rumbleTimeLeft) {
            currentPower = (power * ((rumbleTimeLeft - currentTime) / rumbleTimeLeft)).toFloat()
            rumblePosition.x = (randomFloat() - 0.5f) * 2.0f * currentPower
            rumblePosition.y = (randomFloat() - 0.5f) * 2.0f * currentPower
            rumblePosition.z = (randomFloat() - 0.5f) * 2.0f * currentPower
            rumblePosition

            currentTime += delta
        } else
            rumbleTimeLeft = 0.0

        if (rumbleTimeLeft == 0.0)
            rumblePosition.setZero()

        rumbleTransform.setTranslation(rumblePosition)

        update()
    }

    override fun update() {
        position.set(camera.position).add(rumblePosition)
        view.set(camera.view).mul(rumbleTransform)
        projectionView.set(projection).mul(view)
        inverseProjectionView.set(projectionView).invert()
        frustum.update(inverseProjectionView)
    }
}