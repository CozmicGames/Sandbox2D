package engine.graphics

import com.cozmicgames.Kore
import com.cozmicgames.graphics
import com.cozmicgames.graphics.gpu.Texture
import com.cozmicgames.graphics.safeHeight
import com.cozmicgames.graphics.safeWidth
import com.cozmicgames.memory.Memory
import com.cozmicgames.memory.of
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.maths.Camera
import com.cozmicgames.utils.maths.Matrix4x4
import com.cozmicgames.utils.maths.OrthographicCamera
import com.cozmicgames.utils.use

class Graphics2D : Disposable {
    val linearClampSampler = Kore.graphics.createSampler {
        minFilter = Texture.Filter.LINEAR
        magFilter = Texture.Filter.LINEAR
        xWrap = Texture.Wrap.CLAMP
        yWrap = Texture.Wrap.CLAMP
    }

    val linearRepeatSampler = Kore.graphics.createSampler {
        minFilter = Texture.Filter.LINEAR
        magFilter = Texture.Filter.LINEAR
        xWrap = Texture.Wrap.REPEAT
        yWrap = Texture.Wrap.REPEAT
    }

    val pointClampSampler = Kore.graphics.createSampler {
        minFilter = Texture.Filter.NEAREST
        magFilter = Texture.Filter.NEAREST
        xWrap = Texture.Wrap.CLAMP
        yWrap = Texture.Wrap.CLAMP
    }

    val pointRepeatSampler = Kore.graphics.createSampler {
        minFilter = Texture.Filter.NEAREST
        magFilter = Texture.Filter.NEAREST
        xWrap = Texture.Wrap.REPEAT
        yWrap = Texture.Wrap.REPEAT
    }

    val blankTexture = Kore.graphics.createTexture2D(Texture.Format.RGBA8_UNORM, pointClampSampler) {
        Memory.of(Color.WHITE.bits).use {
            setImage(1, 1, it, Texture.Format.RGBA8_UNORM)
        }
    }

    val missingTexture = Kore.graphics.createTexture2D(Texture.Format.RGBA8_UNORM, pointClampSampler) {
        Memory.of(Color.MAGENTA.bits, Color.BLACK.bits, Color.BLACK.bits, Color.MAGENTA.bits).use {
            setImage(2, 2, it, Texture.Format.RGBA8_UNORM)
        }
    }

    val defaultCamera = OrthographicCamera(Kore.graphics.safeWidth, Kore.graphics.safeHeight)

    val missingMaterial = Material().also {
        it.colorTexturePath = "<missing>"
    }

    private val resizeListener = { width: Int, height: Int ->
        defaultCamera.width = width
        defaultCamera.height = height
        defaultCamera.resetPosition()
        defaultCamera.update()
    }

    private val renderer = Renderer(this)

    init {
        Kore.addResizeListener(resizeListener)
    }

    fun <R> render(camera: Camera, block: (Renderer) -> R) = render(camera.projectionView, block)

    fun <R> render(transform: Matrix4x4 = defaultCamera.projectionView, block: (Renderer) -> R): R {
        renderer.begin()
        renderer.cameraTransform = transform
        val result = block(renderer)
        renderer.end()
        return result
    }

    override fun dispose() {
        Kore.removeResizeListener(resizeListener)
        blankTexture.dispose()
        missingTexture.dispose()
        linearClampSampler.dispose()
        linearRepeatSampler.dispose()
        pointClampSampler.dispose()
        pointRepeatSampler.dispose()
        renderer.dispose()
    }
}
