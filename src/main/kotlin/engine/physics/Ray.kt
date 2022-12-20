package engine.physics

import com.cozmicgames.utils.maths.Vector2

data class Ray(val origin: Vector2, val direction: Vector2, var length: Float = 0.0f)