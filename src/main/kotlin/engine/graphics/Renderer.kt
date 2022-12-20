package engine.graphics

import com.cozmicgames.Kore
import com.cozmicgames.graphics
import com.cozmicgames.graphics.IndexDataType
import com.cozmicgames.graphics.Primitive
import com.cozmicgames.graphics.gpu.*
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.collections.DynamicStack
import com.cozmicgames.utils.maths.*
import engine.Game
import engine.graphics.font.GlyphLayout
import engine.graphics.shaders.DefaultShader
import engine.graphics.shaders.Shader
import engine.assets.managers.getShader
import engine.assets.managers.getTexture

class Renderer(graphics: Graphics2D) : Disposable {
    val context = DrawContext()

    private val vertexBuffer = Kore.graphics.createBuffer(GraphicsBuffer.Usage.DYNAMIC)
    private val indexBuffer = Kore.graphics.createBuffer(GraphicsBuffer.Usage.DYNAMIC)
    private val pipelines = hashMapOf<Shader, Pipeline>()
    private val path = VectorPath()
    private val scissorStack = DynamicStack<ScissorRect?>()
    private var forceUpdateUniforms = false
    private var forceUpdateShader = false

    var isActive = false
        private set

    var shader: Shader = DefaultShader
        set(value) {
            if (!forceUpdateShader && value == field)
                return

            flush()

            val pipeline = pipelines.getOrPut(value) { value.createPipeline() }

            Kore.graphics.setPipeline(pipeline)

            forceUpdateUniforms = true
            texture = Game.graphics2d.blankTexture
            cameraTransform = cameraTransform
            flipX = flipX
            flipY = flipY
            forceUpdateUniforms = false

            field = value
        }

    var cameraTransform = graphics.defaultCamera.projectionView
        set(value) {
            if (!forceUpdateUniforms && value == field)
                return

            flush()
            getPipeline(shader)?.getMatrixUniform("uCameraTransform")?.update(value)
            field = value
        }

    var texture: Texture2D = graphics.blankTexture
        set(value) {
            if (!forceUpdateUniforms && value == field)
                return

            flush()
            getPipeline(shader)?.getTexture2DUniform("uTexture")?.update(value)
            field = value
        }

    var flipX = false
        set(value) {
            if (!forceUpdateUniforms && value == field)
                return

            flush()
            getPipeline(shader)?.getBooleanUniform("uFlipX")?.update(value)
            field = value
        }

    var flipY = false
        set(value) {
            if (!forceUpdateUniforms && value == field)
                return

            flush()
            getPipeline(shader)?.getBooleanUniform("uFlipY")?.update(value)
            field = value
        }

    fun getPipeline(shader: Shader) = pipelines[shader]

    fun path(block: VectorPath.(Renderer) -> Unit): VectorPath {
        path.clear()
        block(path, this)
        return path
    }

    fun <R> withCameraTransform(transform: Matrix4x4, block: (Renderer) -> R): R {
        val previous = cameraTransform
        cameraTransform = transform
        val result = block(this)
        cameraTransform = previous
        return result
    }

    fun <R> withFlippedX(flip: Boolean = !flipX, block: (Renderer) -> R): R {
        val previous = this.flipX
        this.flipX = flip
        val result = block(this)
        this.flipX = previous
        return result
    }

    fun <R> withFlippedY(flip: Boolean = !flipY, block: (Renderer) -> R): R {
        val previous = this.flipY
        this.flipY = flip
        val result = block(this)
        this.flipY = previous
        return result
    }

    fun <R> withShader(shader: Shader, block: (Renderer) -> R): R {
        val previous = this.shader
        this.shader = shader
        val result = block(this)
        this.shader = previous
        return result
    }

    fun pushScissor(rect: ScissorRect?) {
        flush()
        Kore.graphics.setScissor(rect)
        scissorStack.push(rect)
    }

    fun popScissor() {
        flush()
        scissorStack.pop()
        Kore.graphics.setScissor(scissorStack.current)
    }

    fun <R> withScissor(rect: ScissorRect?, block: (Renderer) -> R): R {
        pushScissor(rect)
        val result = block(this)
        popScissor()
        return result
    }

    fun pushMatrix(matrix: Matrix3x2) {
        context.pushMatrix(matrix)
    }

    fun popMatrix() {
        context.popMatrix()
    }

    fun <R> withMatrix(matrix: Matrix3x2, block: Renderer.() -> R): R {
        pushMatrix(matrix)
        val result = block(this)
        popMatrix()
        return result
    }

    fun <R> withTransientState(block: Renderer.() -> R): R {
        val shader = this.shader
        val texture = this.texture
        val cameraTransform = this.cameraTransform
        val flipX = this.flipX
        val flipY = this.flipY
        val result = block(this)
        forceUpdateUniforms = true
        this.shader = shader
        this.texture = texture
        this.cameraTransform = cameraTransform
        this.flipX = flipX
        this.flipY = flipY
        forceUpdateUniforms = false
        return result
    }

    fun draw(texture: TextureRegion, x: Float, y: Float, width: Float, height: Float, color: Color = Color.WHITE, rotation: Float = 0.0f) = draw(texture.texture, x, y, width, height, color, rotation, texture.u0, texture.v0, texture.u1, texture.v1)

    fun draw(texture: Texture2D, x: Float, y: Float, width: Float, height: Float, color: Color = Color.WHITE, rotation: Float = 0.0f, u0: Float = 0.0f, v0: Float = 0.0f, u1: Float = 1.0f, v1: Float = 1.0f) {
        require(isActive)

        this.texture = texture

        context.drawRect(x, y, width, height, color, rotation, u0, v0, u1, v1)
    }

    fun drawPathFilled(path: VectorPath, color: Color = Color.WHITE, texture: Texture2D = Game.graphics2d.blankTexture, vMin: Float = 0.0f, uMin: Float = 0.0f, uMax: Float = 1.0f, vMax: Float = 1.0f) {
        require(isActive)

        this.texture = texture
        context.drawPathFilled(path, color, uMin, vMin, uMax, vMax)
    }

    fun drawPathStroke(path: VectorPath, thickness: Float, closed: Boolean, color: Color = Color.WHITE, extrusionOffset: Float = 0.5f) {
        require(isActive)

        this.texture = Game.graphics2d.blankTexture
        context.drawPathStroke(path, thickness, closed, color, extrusionOffset)
    }

    fun drawGlyphs(glyphLayout: GlyphLayout, x: Float, y: Float, color: Color = Color.WHITE) {
        require(isActive)

        val shader = Game.assets.getShader(glyphLayout.font.requiredShader) ?: DefaultShader

        withShader(shader) {
            getPipeline(shader)?.let {
                glyphLayout.font.setUniforms(it)
            }
            texture = glyphLayout.font.texture
            context.drawGlyphs(glyphLayout, x, y, color)
        }
    }

    fun drawGlyphsClipped(glyphLayout: GlyphLayout, x: Float, y: Float, clipRect: Rectangle, color: Color = Color.WHITE) {
        require(isActive)

        val shader = Game.assets.getShader(glyphLayout.font.requiredShader) ?: DefaultShader

        withShader(shader) {
            getPipeline(shader)?.let {
                glyphLayout.font.setUniforms(it)
            }
            texture = glyphLayout.font.texture
            context.drawGlyphsClipped(glyphLayout, x, y, clipRect, color)
        }
    }

    fun drawTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, color0: Color, color1: Color = color0, color2: Color = color0) {
        require(isActive)

        texture = Game.graphics2d.blankTexture

        context.drawTriangle(x0, y0, x1, y1, x2, y2, color0, color1, color2)
    }

    fun drawRect(x: Float, y: Float, width: Float, height: Float, color00: Color, color01: Color = color00, color11: Color = color00, color10: Color = color00) {
        require(isActive)

        texture = Game.graphics2d.blankTexture

        context.drawRect(x, y, width, height, color00, color01, color11, color10)
    }

    fun drawBatch(batch: RenderBatch) {
        require(isActive)

        val material = batch.material

        if (material != null) {
            shader = Game.assets.getShader(material.shader) ?: DefaultShader
            texture = (Game.assets.getTexture(material.colorTexturePath)).texture
            shader.setMaterial(material)
            context.draw(batch.context)
        } else {
            shader = DefaultShader
            texture = Game.graphics2d.missingTexture
            context.draw(batch.context)
        }

        flush() //TODO: Find out why this is needed here to actually draw all layers in a scene
        shader = DefaultShader
    }

    fun begin() {
        if (isActive)
            return

        isActive = true

        forceUpdateShader = true
        shader = DefaultShader
        forceUpdateShader = false

        Kore.graphics.setVertexBuffer(vertexBuffer, Shader.VERTEX_LAYOUT.indices)
        Kore.graphics.setIndexBuffer(indexBuffer)
    }

    fun end() {
        if (!isActive)
            return

        flush()

        context.reset()

        Kore.graphics.setPipeline(null)
        Kore.graphics.setVertexBuffer(null)
        Kore.graphics.setIndexBuffer(null)

        isActive = false
    }

    fun flush() {
        if (context.numVertices == 0 || context.numIndices == 0)
            return

        context.updateBuffers(vertexBuffer, indexBuffer)

        Kore.graphics.drawIndexed(Primitive.TRIANGLES, context.numIndices, 0, IndexDataType.INT)

        context.reset()
    }

    override fun dispose() {
        context.dispose()
        vertexBuffer.dispose()
        indexBuffer.dispose()
    }
}

fun Renderer.setCamera(camera: Camera) {
    cameraTransform = camera.projectionView
}

fun <R> Renderer.withCamera(camera: Camera, block: (Renderer) -> R) = withCameraTransform(camera.projectionView, block)
