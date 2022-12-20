package engine.graphics.rendergraph

import com.cozmicgames.Kore
import com.cozmicgames.graphics
import com.cozmicgames.graphics.gpu.Framebuffer
import com.cozmicgames.graphics.gpu.Texture
import com.cozmicgames.graphics.gpu.Texture2D
import com.cozmicgames.utils.Disposable
import engine.Game

open class RenderPass(val resolution: Resolution, block: RenderPass.() -> Unit = {}) : Disposable {
    interface Resolution {
        fun getWidth(totalWidth: Int = Kore.graphics.width): Int
        fun getHeight(totalHeight: Int = Kore.graphics.height): Int
    }

    object StandardResolution : Resolution {
        override fun getWidth(totalWidth: Int) = totalWidth
        override fun getHeight(totalHeight: Int) = totalHeight
    }

    class AbsoluteResolution(val width: Int, val height: Int) : Resolution {
        override fun getWidth(totalWidth: Int) = width
        override fun getHeight(totalHeight: Int) = height
    }

    class ScaledResolution(val scaleWidth: Float, val scaleHeight: Float) : Resolution {
        companion object {
            val HALF = ScaledResolution(0.5f)
            val ONE_THIRD = ScaledResolution(1.0f / 3.0f)
            val ONE_FOURTH = ScaledResolution(0.25f)
            val TWO_THIRDS = ScaledResolution(2.0f / 3.0f)
            val ONE_EIGHTH = ScaledResolution(0.125f)
        }

        constructor(scale: Float) : this(scale, scale)

        override fun getWidth(totalWidth: Int) = (totalWidth * scaleWidth).toInt()
        override fun getHeight(totalHeight: Int) = (totalHeight * scaleHeight).toInt()
    }

    class RenderTarget(val pass: RenderPass, val type: Type, val format: Texture.Format = getStandardFormat(type)) {
        companion object {
            fun getStandardFormat(type: Type) = when (type) {
                Type.COLOR -> Texture.Format.RGBA8_UNORM
                Type.DEPTH -> Texture.Format.DEPTH24
                Type.STENCIL -> Texture.Format.STENCIL8
            }
        }

        enum class Type {
            COLOR,
            DEPTH,
            STENCIL
        }

        var colorIndex = 0
            internal set
    }

    val colorRenderTargets: List<RenderTarget> get() = internalColorRenderTargets

    var depthRenderTarget: RenderTarget? = null
        set(value) {
            field = value
            value?.colorIndex = 0
            isDirty = true
        }

    var stencilRenderTarget: RenderTarget? = null
        set(value) {
            field = value
            value?.colorIndex = 0
            isDirty = true
        }

    val hasColorRenderTargets get() = internalColorRenderTargets.isNotEmpty()
    val hasDepthRenderTarget get() = depthRenderTarget != null
    val hasStencilRenderTarget get() = stencilRenderTarget != null

    private val internalColorRenderTargets = arrayListOf<RenderTarget>()

    private var framebuffer = Kore.graphics.createFramebuffer()
    private var isDirty = true
    private var isResized = false
    private var resizeWidth = Kore.graphics.width
    private var resizeHeight = Kore.graphics.height

    var width = 0
        private set

    var height = 0
        private set

    var name = "???"
        internal set

    init {
        block(this)
    }

    private fun updateColorRenderTargetIndices() {
        colorRenderTargets.forEachIndexed { index, renderTarget ->
            renderTarget.colorIndex = index
        }
    }

    fun getColorTexture(index: Int): Texture2D? = if (hasColorRenderTargets) framebuffer[when (index) {
        0 -> Framebuffer.Attachment.COLOR0
        1 -> Framebuffer.Attachment.COLOR1
        2 -> Framebuffer.Attachment.COLOR2
        3 -> Framebuffer.Attachment.COLOR3
        4 -> Framebuffer.Attachment.COLOR4
        5 -> Framebuffer.Attachment.COLOR5
        6 -> Framebuffer.Attachment.COLOR6
        7 -> Framebuffer.Attachment.COLOR7
        else -> throw RuntimeException("Too many color rendertargets, up to 8 are supported")
    }] else null

    fun getDepthTexture(): Texture2D? = if (hasDepthRenderTarget) framebuffer[Framebuffer.Attachment.DEPTH] else null

    fun getStencilTexture(): Texture2D? = if (hasStencilRenderTarget) framebuffer[Framebuffer.Attachment.STENCIL] else null

    fun addColorRenderTarget(format: Texture.Format = RenderTarget.getStandardFormat(RenderTarget.Type.COLOR)): RenderTarget {
        val target = RenderTarget(this, RenderTarget.Type.COLOR, format)
        internalColorRenderTargets += target
        isDirty = true
        return target
    }

    fun removeColorRenderTarget(renderTarget: RenderTarget) {
        internalColorRenderTargets -= renderTarget
        isDirty = true
    }

    fun resize(width: Int, height: Int) {
        isResized = true
        this.resizeWidth = width
        this.resizeHeight = height
    }

    fun begin() {
        if (isDirty) {
            framebuffer.dispose()
            framebuffer = Kore.graphics.createFramebuffer()

            updateColorRenderTargetIndices()

            colorRenderTargets.forEach {
                val attachment = when (it.colorIndex) {
                    0 -> Framebuffer.Attachment.COLOR0
                    1 -> Framebuffer.Attachment.COLOR1
                    2 -> Framebuffer.Attachment.COLOR2
                    3 -> Framebuffer.Attachment.COLOR3
                    4 -> Framebuffer.Attachment.COLOR4
                    5 -> Framebuffer.Attachment.COLOR5
                    6 -> Framebuffer.Attachment.COLOR6
                    7 -> Framebuffer.Attachment.COLOR7
                    else -> throw RuntimeException("Too many color rendertargets, up to 8 are supported")
                }

                framebuffer.addAttachment(attachment, it.format, Game.graphics2d.pointClampSampler)
            }

            depthRenderTarget?.let {
                framebuffer.addAttachment(Framebuffer.Attachment.DEPTH, it.format, Game.graphics2d.pointClampSampler)
            }

            stencilRenderTarget?.let {
                framebuffer.addAttachment(Framebuffer.Attachment.STENCIL, it.format, Game.graphics2d.pointClampSampler)
            }

            width = resolution.getWidth(this.resizeWidth)
            height = resolution.getHeight(this.resizeHeight)

            framebuffer.update(width, height)

            isDirty = false
        }

        if (isResized) {
            val width = resolution.getWidth(this.resizeWidth)
            val height = resolution.getHeight(this.resizeHeight)

            framebuffer.update(width, height)

            isResized = false
        }

        Kore.graphics.setFramebuffer(framebuffer)
    }

    override fun dispose() {
        framebuffer.dispose()
        name = "???"
    }
}

val RenderPass.aspectRatio get() = width.toFloat() / height.toFloat()

fun standardResolution() = RenderPass.StandardResolution

fun absoluteResolution(width: Int, height: Int) = RenderPass.AbsoluteResolution(width, height)

fun absoluteResolution(size: Int) = RenderPass.AbsoluteResolution(size, size)

fun scaledResolution(scaleWidth: Float, scaleHeight: Float) = RenderPass.ScaledResolution(scaleWidth, scaleHeight)

fun scaledResolution(scale: Float) = RenderPass.ScaledResolution(scale)

fun RenderPass.depthRenderTarget(format: Texture.Format = Texture.Format.DEPTH24) = RenderPass.RenderTarget(this, RenderPass.RenderTarget.Type.DEPTH, format)

fun RenderPass.stencilRenderTarget(format: Texture.Format = Texture.Format.STENCIL8) = RenderPass.RenderTarget(this, RenderPass.RenderTarget.Type.STENCIL, format)

fun RenderPass.addDepthRenderTarget(format: Texture.Format = Texture.Format.DEPTH24): RenderPass.RenderTarget {
    val target = depthRenderTarget(format)
    depthRenderTarget = target
    return target
}

fun RenderPass.addStencilRenderTarget(format: Texture.Format = Texture.Format.STENCIL8): RenderPass.RenderTarget {
    val target = stencilRenderTarget(format)
    stencilRenderTarget = target
    return target
}