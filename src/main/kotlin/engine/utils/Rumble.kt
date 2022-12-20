package engine.utils

import com.cozmicgames.utils.Updateable
import com.cozmicgames.utils.maths.Vector3
import com.cozmicgames.utils.maths.randomFloat

class Rumble : Updateable {
    private var rumbleTimeLeft = 0.0
    private var currentTime = 0.0f
    private var power = 0.0f
    private var currentPower = 0.0f

    val position = Vector3()

    fun rumble(power: Float, duration: Double) {
        this.power = power
        rumbleTimeLeft = duration
        currentTime = 0.0f
    }

    override fun update(delta: Float) {
        if (currentTime <= rumbleTimeLeft) {
            currentPower = (power * ((rumbleTimeLeft - currentTime) / rumbleTimeLeft)).toFloat()
            position.x = (randomFloat() - 0.5f) * 2.0f * currentPower
            position.y = (randomFloat() - 0.5f) * 2.0f * currentPower
            position.z = (randomFloat() - 0.5f) * 2.0f * currentPower
            position

            currentTime += delta
        } else
            rumbleTimeLeft = 0.0

        if (rumbleTimeLeft == 0.0)
            position.setZero()
    }
}