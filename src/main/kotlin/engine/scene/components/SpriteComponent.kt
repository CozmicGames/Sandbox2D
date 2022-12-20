package engine.scene.components

import com.cozmicgames.utils.Properties
import com.cozmicgames.utils.Updateable
import engine.graphics.Drawable
import engine.graphics.Bounds
import engine.scene.Component

class SpriteComponent : Component(), Updateable, Drawable {
    val bounds = Bounds()

    var isFlippedX = false
    var isFlippedY = false

    override var layer = 0
    override var material: String? = null

    var anchorX = 0.5f
        set(value) {
            field = value
            isDirty = true
        }

    var anchorY = 0.5f
        set(value) {
            field = value
            isDirty = true
        }

    override val vertices = arrayOf(
        Drawable.Vertex(0.0f - anchorX, 0.0f - anchorY, 0.0f, 1.0f),
        Drawable.Vertex(1.0f - anchorX, 0.0f - anchorY, 1.0f, 1.0f),
        Drawable.Vertex(1.0f - anchorX, 1.0f - anchorY, 1.0f, 0.0f),
        Drawable.Vertex(0.0f - anchorX, 1.0f - anchorY, 0.0f, 0.0f)
    )

    override val indices = arrayOf(0, 1, 2, 0, 2, 3)

    private lateinit var transformComponent: TransformComponent
    private var isDirty = true

    override fun onAdded() {
        transformComponent = gameObject.getOrAddComponent()
        transformComponent.transform.addChangeListener {
            isDirty = true
        }
    }

    override fun update(delta: Float) {
        if (isDirty) {
            updateVertices()
            isDirty = false
        }
    }

    private fun updateVertices() {
        transformComponent.transform.transform(0.0f - anchorX, 0.0f - anchorY) { x, y ->
            vertices[0].x = x
            vertices[0].y = y
        }

        transformComponent.transform.transform(1.0f - anchorX, 0.0f - anchorY) { x, y ->
            vertices[1].x = x
            vertices[1].y = y
        }

        transformComponent.transform.transform(1.0f - anchorX, 1.0f - anchorY) { x, y ->
            vertices[2].x = x
            vertices[2].y = y
        }

        transformComponent.transform.transform(0.0f - anchorX, 1.0f - anchorY) { x, y ->
            vertices[3].x = x
            vertices[3].y = y
        }

        bounds.update(vertices[0].x, vertices[0].y, vertices[1].x, vertices[1].y, vertices[2].x, vertices[2].y, vertices[3].x, vertices[3].y)
    }

    override fun read(properties: Properties) {
        properties.getBoolean("isFlippedX")?.let { isFlippedX = it }
        properties.getBoolean("isFlippedY")?.let { isFlippedY = it }
        properties.getInt("layer")?.let { layer = it }
        properties.getString("material")?.let { material = it }
        properties.getFloat("anchorX")?.let { anchorX = it }
        properties.getFloat("anchorY")?.let { anchorY = it }
    }

    override fun write(properties: Properties) {
        properties.setBoolean("isFlippedX", isFlippedX)
        properties.setBoolean("isFlippedY", isFlippedY)
        properties.setInt("layer", layer)
        material?.let { properties.setString("material", it) }
        properties.setFloat("anchorX", anchorX)
        properties.setFloat("anchorY", anchorY)
    }
}