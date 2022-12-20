package engine.scene.components

import com.cozmicgames.utils.Properties
import com.cozmicgames.utils.Updateable
import engine.physics.*
import engine.scene.Component
import engine.scene.processors.PhysicsProcessor

class ColliderComponent : Updateable, Component() {
    var isStatic = false
        set(value) {
            if (value == field)
                return

            field = value
            isDirty = true
        }

    var density = 1.0f
        set(value) {
            if (value == field)
                return

            field = value
            isDirty = true
        }

    var restitution = 0.0f
        set(value) {
            if (value == field)
                return

            field = value
            isDirty = true
        }

    var staticFriction = 0.5f
        set(value) {
            if (value == field)
                return

            field = value
            isDirty = true
        }

    var dynamicFriction = 0.3f
        set(value) {
            if (value == field)
                return

            field = value
            isDirty = true
        }

    var gravityScale = 1.0f
        set(value) {
            if (value == field)
                return

            field = value
            isDirty = true
        }

    var shape: Shape = RectangleShape()
        set(value) {
            field = value
            isDirty = true
        }

    var body: Body? = null
        private set

    private var isDirty = true


    override fun onActiveChanged() {
        if (gameObject.isActive)
            isDirty = true
        else {
            val physicsProcessor = gameObject.scene.findSceneProcessor<PhysicsProcessor>() ?: return
            body?.let {
                physicsProcessor.physics.removeBody(it)
            }
        }
    }

    override fun update(delta: Float) {
        if (!isDirty)
            return

        val physicsProcessor = gameObject.scene.findSceneProcessor<PhysicsProcessor>() ?: return

        this.body?.let {
            physicsProcessor.physics.removeBody(it)
        }

        val transformComponent = gameObject.getOrAddComponent<TransformComponent>()

        val body = Body(transformComponent.transform)
        body.setShape(shape, density)

        if (isStatic)
            body.setStatic()

        body.restitution = restitution
        body.staticFriction = staticFriction
        body.dynamicFriction = dynamicFriction
        body.gravityScale = gravityScale

        physicsProcessor.physics.addBody(body)
    }

    override fun read(properties: Properties) {
        properties.getBoolean("isStatic")?.let { isStatic = it }
        properties.getFloat("density")?.let { density = it }
        properties.getFloat("restitution")?.let { restitution = it }
        properties.getFloat("staticFriction")?.let { staticFriction = it }
        properties.getFloat("dynamicFriction")?.let { dynamicFriction = it }
        properties.getFloat("gravityScale")?.let { gravityScale = it }

        properties.getProperties("shape")?.let {
            val type = it.getString("type")

            shape = when (type?.lowercase()) {
                "axisalignedrectangle" -> {
                    val x = it.getFloat("centerX") ?: 0.0f
                    val y = it.getFloat("centerY") ?: 0.0f
                    val width = it.getFloat("width") ?: 1.0f
                    val height = it.getFloat("height") ?: 1.0f

                    AxisAlignedRectangleShape().also {
                        it.centerX = x
                        it.centerY = y
                        it.width = width
                        it.height = height
                    }
                }
                "circle" -> {
                    val x = it.getFloat("centerX") ?: 0.0f
                    val y = it.getFloat("centerY") ?: 0.0f
                    val radius = it.getFloat("radius") ?: 0.5f

                    CircleShape().also {
                        it.x = x
                        it.y = y
                        it.radius = radius
                    }
                }
                "polygon" -> {
                    val shape = PolygonShape()

                    it.getProperties("translation")?.let {
                        shape.translation.x = it.getFloat("x") ?: 0.0f
                        shape.translation.y = it.getFloat("y") ?: 0.0f
                    }

                    it.getProperties("scale")?.let {
                        shape.scale.x = it.getFloat("x") ?: 0.0f
                        shape.scale.y = it.getFloat("y") ?: 0.0f
                    }

                    it.getFloat("rotation")?.let {
                        shape.rotation.setRotation(it)
                        shape.transposedRotation.set(shape.rotation).transpose()
                    }

                    val vertices = arrayListOf<PolygonShape.Vertex>()

                    it.getPropertiesArray("vertices")?.let {
                        it.forEach {
                            vertices += shape.Vertex {
                                it.getProperties("position")?.let {
                                    position.x = it.getFloat("x") ?: 0.0f
                                    position.y = it.getFloat("y") ?: 0.0f
                                }
                                it.getProperties("scale")?.let {
                                    normal.x = it.getFloat("x") ?: 0.0f
                                    normal.y = it.getFloat("y") ?: 0.0f
                                }
                            }
                        }
                    }

                    shape.also { it.setVertices(*vertices.toTypedArray()) }
                }
                else /* "rectangle" */ -> {
                    val shape = RectangleShape()

                    it.getProperties("translation")?.let {
                        shape.translation.x = it.getFloat("x") ?: 0.0f
                        shape.translation.y = it.getFloat("y") ?: 0.0f
                    }

                    it.getProperties("scale")?.let {
                        shape.scale.x = it.getFloat("x") ?: 0.0f
                        shape.scale.y = it.getFloat("y") ?: 0.0f
                    }

                    it.getFloat("rotation")?.let {
                        shape.rotation.setRotation(it)
                        shape.transposedRotation.set(shape.rotation).transpose()
                    }

                    val width = it.getFloat("width") ?: 1.0f
                    val height = it.getFloat("height") ?: 1.0f

                    shape.also {
                        it.width = width
                        it.height = height
                    }
                }
            }
        }

        isDirty = true
    }

    override fun write(properties: Properties) {
        properties.setBoolean("isStatic", isStatic)
        properties.setFloat("density", density)
        properties.setFloat("restitution", restitution)
        properties.setFloat("staticFriction", staticFriction)
        properties.setFloat("dynamicFriction", dynamicFriction)
        properties.setFloat("gravityScale", gravityScale)

        val shapeProperties = Properties()

        when (shape) {
            is AxisAlignedRectangleShape -> {
                shapeProperties.setString("type", "axisalignedrectangle")
                shapeProperties.setFloat("centerX", (shape as AxisAlignedRectangleShape).centerX)
                shapeProperties.setFloat("centerY", (shape as AxisAlignedRectangleShape).centerY)
                shapeProperties.setFloat("width", (shape as AxisAlignedRectangleShape).width)
                shapeProperties.setFloat("height", (shape as AxisAlignedRectangleShape).height)
            }
            is CircleShape -> {
                shapeProperties.setString("type", "circle")
                shapeProperties.setFloat("x", (shape as CircleShape).x)
                shapeProperties.setFloat("y", (shape as CircleShape).y)
                shapeProperties.setFloat("radius", (shape as CircleShape).radius)
            }
            is PolygonShape -> {
                shapeProperties.setString("type", "polygon")

                shapeProperties.setProperties("translation", Properties().also {
                    it.setFloat("x", (shape as PolygonShape).translation.x)
                    it.setFloat("y", (shape as PolygonShape).translation.y)
                })

                shapeProperties.setProperties("scale", Properties().also {
                    it.setFloat("x", (shape as PolygonShape).scale.x)
                    it.setFloat("y", (shape as PolygonShape).scale.y)
                })

                shapeProperties.setFloat("rotation", (shape as PolygonShape).rotation.getRotation())

                val verticesProperties = arrayListOf<Properties>()

                for (vertex in (shape as PolygonShape).vertices) {
                    val vertexProperties = Properties()

                    vertexProperties.setProperties("position", Properties().also {
                        it.setFloat("x", vertex.position.x)
                        it.setFloat("y", vertex.position.y)
                    })

                    vertexProperties.setProperties("normal", Properties().also {
                        it.setFloat("x", vertex.normal.x)
                        it.setFloat("y", vertex.normal.y)
                    })

                    verticesProperties += vertexProperties
                }

                shapeProperties.setPropertiesArray("vertices", verticesProperties.toTypedArray())
            }
            else /* is RectangleShape */ -> {
                shapeProperties.setString("type", "rectangle")

                shapeProperties.setProperties("translation", Properties().also {
                    it.setFloat("x", (shape as PolygonShape).translation.x)
                    it.setFloat("y", (shape as PolygonShape).translation.y)
                })

                shapeProperties.setProperties("scale", Properties().also {
                    it.setFloat("x", (shape as PolygonShape).scale.x)
                    it.setFloat("y", (shape as PolygonShape).scale.y)
                })

                shapeProperties.setFloat("rotation", (shape as PolygonShape).rotation.getRotation())

                shapeProperties.setFloat("width", (shape as RectangleShape).width)
                shapeProperties.setFloat("height", (shape as RectangleShape).height)
            }
        }

        properties.setProperties("shape", shapeProperties)
    }
}