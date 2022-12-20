package engine.graphics.ui

import com.cozmicgames.Kore
import com.cozmicgames.graphics
import com.cozmicgames.graphics.gpu.ScissorRect
import com.cozmicgames.graphics.gpu.Texture2D
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.collections.Resettable
import com.cozmicgames.utils.maths.*
import engine.graphics.Renderer
import engine.graphics.TextureRegion
import engine.graphics.font.GlyphLayout

open class GUICommandList : Resettable {
    protected open val commands: MutableList<Renderer.() -> Unit>? = arrayListOf()

    val isEmpty get() = commands?.isNotEmpty() == false

    fun addCommand(command: Renderer.() -> Unit) {
        commands?.add(command)
    }

    fun addCommandList(list: GUICommandList) {
        list.commands?.let {
            commands?.addAll(it)
            it.clear()
        }
    }

    fun process(renderer: Renderer) {
        commands?.forEach {
            it(renderer)
        }
        commands?.clear()
    }

    override fun reset() {
        commands?.clear()
    }
}

object GUINoopCommandList : GUICommandList() {
    override val commands = null
}

fun GUICommandList.pushScissor(x: Float, y: Float, width: Float, height: Float) = addCommand {
    pushScissor(ScissorRect(x.toInt(), Kore.graphics.height - (y.toInt() + height.toInt()), width.toInt(), height.toInt()))
}

fun GUICommandList.popScissor() = addCommand {
    popScissor()
}

fun GUICommandList.pushMatrix(matrix: Matrix3x2) = addCommand {
    pushMatrix(matrix)
}

fun GUICommandList.popMatrix() = addCommand {
    popMatrix()
}

fun GUICommandList.drawLine(x0: Float, y0: Float, x1: Float, y1: Float, thickness: Float, color: Color) = addCommand {
    drawPathStroke(path {
        line(x0, y0, x1, y1)
    }, thickness, false, color)
}

fun GUICommandList.drawCurve(x0: Float, y0: Float, x1: Float, y1: Float, controlX0: Float, contronY0: Float, controlX1: Float, contronY1: Float, thickness: Float, color: Color) = addCommand {
    drawPathStroke(path {
        bezier(controlX0, contronY0, controlX1, contronY1, x1, y1)
    }, thickness, false, color)
}

fun GUICommandList.drawRect(x: Float, y: Float, width: Float, height: Float, roundedCorners: Int, cornerRounding: Float, thickness: Float, color: Color) = addCommand {
    drawPathStroke(path {
        if (roundedCorners != Corners.NONE)
            roundedRect(x, y, width, height, cornerRounding, roundedCorners)
        else
            rect(x, y, width, height)
    }, thickness, true, color, 0.0f)
}

fun GUICommandList.drawRectFilled(x: Float, y: Float, width: Float, height: Float, roundedCorners: Int, cornerRounding: Float, color: Color) = addCommand {
    drawPathFilled(path {
        if (roundedCorners != Corners.NONE)
            roundedRect(x, y, width, height, cornerRounding, roundedCorners)
        else
            rect(x, y, width, height)
    }, color)
}

fun GUICommandList.drawRectMultiColor(x: Float, y: Float, width: Float, height: Float, color00: Color, color01: Color, color11: Color, color10: Color) = addCommand {
    drawRect(x, y, width, height, color00, color01, color11, color10)
}

fun GUICommandList.drawTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Color) = addCommand {
    drawPathStroke(path {
        add(x0, y0)
        add(x1, y1)
        add(x2, y2)
    }, thickness, true, color)
}

fun GUICommandList.drawTriangleFilled(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, color: Color) = addCommand {
    drawPathFilled(path {
        add(x0, y0)
        add(x1, y1)
        add(x2, y2)
    }, color)
}

fun GUICommandList.drawCircle(x: Float, y: Float, radius: Float, thickness: Float, color: Color) = addCommand {
    drawPathStroke(path {
        circle(x, y, radius)
    }, thickness, true, color)
}

fun GUICommandList.drawCircleFilled(x: Float, y: Float, radius: Float, color: Color) = addCommand {
    drawPathFilled(path {
        circle(x, y, radius)
    }, color)
}

fun GUICommandList.drawArc(x: Float, y: Float, radius: Float, angleMin: Float, angleMax: Float, thickness: Float, color: Color) = addCommand {
    drawPathStroke(path {
        arc(x, y, radius, angleMin, angleMax)
    }, thickness, false, color)
}

fun GUICommandList.drawArcFilled(x: Float, y: Float, radius: Float, angleMin: Float, angleMax: Float, color: Color) = addCommand {
    drawPathFilled(path {
        add(x, y)
        arc(x, y, radius, angleMin, angleMax)
    }, color)
}

fun GUICommandList.drawPolygon(points: Iterable<Vector2>, thickness: Float, color: Color) = addCommand {
    drawPathStroke(path {
        points.forEach {
            add(it.x, it.y)
        }
    }, thickness, true, color)
}

fun GUICommandList.drawPolygonFilled(points: Iterable<Vector2>, color: Color) = addCommand {
    drawPathFilled(path {
        points.forEach {
            add(it.x, it.y)
        }
    }, color)
}

fun GUICommandList.drawPolyline(points: Iterable<Vector2>, thickness: Float, color: Color) = addCommand {
    drawPathStroke(path {
        points.forEach {
            add(it.x, it.y)
        }
    }, thickness, false, color)
}

fun GUICommandList.drawImage(x: Float, y: Float, width: Float, height: Float, texture: Texture2D, u0: Float, v0: Float, u1: Float, v1: Float, color: Color) = drawImage(x, y, width, height, TextureRegion(texture, u0, v0, u1, v1), color)

fun GUICommandList.drawImage(x: Float, y: Float, width: Float, height: Float, region: TextureRegion, color: Color) = addCommand {
    draw(region.texture, x, y, width, height, color, u0 = region.u0, v0 = region.v1, u1 = region.u1, v1 = region.v0)
}

fun GUICommandList.drawText(x: Float, y: Float, layout: GlyphLayout, foregroundColor: Color, clipRect: Rectangle? = null) = addCommand {
    if (clipRect != null)
        drawGlyphsClipped(layout, x, y, clipRect, foregroundColor)
    else
        drawGlyphs(layout, x, y, foregroundColor)
}

fun GUICommandList.drawPath(path: VectorPath, thickness: Float, closed: Boolean, color: Color) = addCommand {
    drawPathStroke(path, thickness, closed, color)
}

fun GUICommandList.drawPathFilled(path: VectorPath, color: Color) = addCommand {
    drawPathFilled(path, color)
}

fun GUICommandList.drawStretchingImage(x: Float, y: Float, width: Float, height: Float, texture: TextureRegion, color: Color = Color.WHITE, left: Float = 1.0f / 3.0f, right: Float = 1.0f / 3.0f, top: Float = 1.0f / 3.0f, bottom: Float = 1.0f / 3.0f) = addCommand {
    val u0 = 0.0f
    val u1 = u0 + left
    val u2 = 1.0f - right
    val u3 = 1.0f

    val v0 = 0.0f
    val v1 = v0 + top
    val v2 = 1.0f - bottom
    val v3 = 1.0f

    val texture00 = texture.getSubRegion(u0, v0, u1, v1)
    val texture01 = texture.getSubRegion(u0, v1, u1, v2)
    val texture02 = texture.getSubRegion(u0, v2, u1, v3)
    val texture10 = texture.getSubRegion(u1, v0, u2, v1)
    val texture11 = texture.getSubRegion(u1, v1, u2, v2)
    val texture12 = texture.getSubRegion(u1, v2, u2, v3)
    val texture20 = texture.getSubRegion(u2, v0, u3, v1)
    val texture21 = texture.getSubRegion(u2, v1, u3, v2)
    val texture22 = texture.getSubRegion(u2, v2, u3, v3)

    val x0 = x
    val x1 = x0 + texture.width * left
    val x2 = x0 + width - texture.width * right

    val y0 = y + height - texture.height * bottom
    val y1 = y + texture.height * top
    val y2 = y


    val w0 = texture.width * left
    val w2 = texture.width * right
    val w1 = width - w0 - w2

    val h0 = texture.height * top
    val h2 = texture.height * bottom
    val h1 = height - h0 - h2

    draw(texture00, x0, y0, w0, h0, color)
    draw(texture01, x0, y1, w0, h1, color)
    draw(texture02, x0, y2, w0, h2, color)
    draw(texture10, x1, y0, w1, h0, color)
    draw(texture11, x1, y1, w1, h1, color)
    draw(texture12, x1, y2, w1, h2, color)
    draw(texture20, x2, y0, w2, h0, color)
    draw(texture21, x2, y1, w2, h1, color)
    draw(texture22, x2, y2, w2, h2, color)
}
