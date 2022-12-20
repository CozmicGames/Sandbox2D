package engine.graphics.particles.data

import com.cozmicgames.utils.Color
import engine.graphics.particles.ParticleData

data class ColorData(val color: Color = Color(), val startColor: Color = Color(), val endColor: Color = Color()) : ParticleData.DataType

