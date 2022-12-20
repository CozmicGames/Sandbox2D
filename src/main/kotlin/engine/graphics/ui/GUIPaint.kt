package engine.graphics.ui

import com.cozmicgames.utils.Color
import com.cozmicgames.utils.maths.Vector2
import engine.graphics.TextureRegion
import engine.utils.Gradient

class GUIPaint {
    var color: Color? = null
    var texture: TextureRegion? = null
    var gradient: Gradient? = null
    val gradientDirection = Vector2(0.0f, 1.0f)
}
