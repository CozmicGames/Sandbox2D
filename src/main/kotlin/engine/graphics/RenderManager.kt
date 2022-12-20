package engine.graphics

import com.cozmicgames.graphics.gpu.Texture2D
import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.collections.Pool
import com.cozmicgames.utils.maths.OrthographicCamera
import engine.Game
import engine.graphics.shaders.DefaultShader
import engine.assets.managers.getMaterial
import engine.assets.managers.getShader

class RenderManager : Disposable {
    private class RenderList(val layer: Int) : Iterable<Renderable> {
        private val renderables = arrayListOf<Renderable>()

        override fun iterator() = renderables.iterator()

        fun add(renderable: Renderable) {
            renderables += renderable
        }

        fun clear() {
            renderables.clear()
        }
    }

    private val renderLists = arrayListOf<RenderList>()
    private val drawableRenderablePool = Pool(supplier = { DrawableRenderable() })
    private val directRenderablePool = Pool(supplier = { DirectRenderable() })
    private val renderables = arrayListOf<Renderable>()
    private val batchBuilder = RenderBatchBuilder()

    fun submit(drawable: Drawable, flipX: Boolean, flipY: Boolean) {
        val renderable = drawableRenderablePool.obtain()
        renderable.drawable = drawable
        renderable.material = drawable.material?.let { Game.assets.getMaterial(it) } ?: Game.graphics2d.missingMaterial
        renderable.layer = drawable.layer
        renderable.flipX = flipX
        renderable.flipY = flipY
        renderable.updateBounds()
        renderables += renderable
    }

    fun submit(layer: Int, texture: Texture2D, shader: String, flipX: Boolean, flipY: Boolean, draw: (DrawContext) -> Unit) {
        val renderable = directRenderablePool.obtain()
        draw(renderable.context)
        renderable.flipX = flipX
        renderable.flipY = flipY
        renderable.texture = texture
        renderable.shader = shader
        renderable.layer = layer
        renderable.updateBounds()
        renderables += renderable
    }

    fun render(camera: OrthographicCamera, layerFilter: (Int) -> Boolean = { true }) {
        for (renderable in renderables) {
            if (!(renderable.bounds intersects camera.rectangle))
                continue

            if (!layerFilter(renderable.layer))
                continue

            var renderList: RenderList? = null

            for (list in renderLists)
                if (list.layer == renderable.layer) {
                    renderList = list
                    break
                }

            if (renderList == null) {
                renderList = RenderList(renderable.layer)
                renderLists += renderList
                renderLists.sortBy { it.layer }
            }

            renderList.add(renderable)
        }

        Game.graphics2d.render(camera) { renderer ->
            renderLists.forEach {
                for (renderable in it)
                    when (renderable) {
                        is DrawableRenderable -> batchBuilder.submit(renderable.material, renderable.drawable, renderable.flipX, renderable.flipY)
                        is DirectRenderable -> {
                            renderer.withTransientState {
                                shader = Game.assets.getShader(renderable.shader) ?: DefaultShader
                                texture = renderable.texture
                                flipX = renderable.flipX
                                flipY = renderable.flipY
                                context.draw(renderable.context)
                            }
                        }
                    }

                it.clear()

                batchBuilder.flush { batch ->
                    renderer.drawBatch(batch)
                }
            }
        }
    }

    fun clear() {
        renderables.forEach {
            when (it) {
                is DrawableRenderable -> drawableRenderablePool.free(it)
                is DirectRenderable -> directRenderablePool.free(it)
            }
        }
        renderables.clear()
    }

    override fun dispose() {
        batchBuilder.dispose()
        drawableRenderablePool.dispose()
        directRenderablePool.dispose()
    }
}