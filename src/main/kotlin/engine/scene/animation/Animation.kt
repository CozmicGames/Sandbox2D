package engine.scene.animation

import com.cozmicgames.utils.Properties

class Animation {
    var name = ""
    var duration = 0.0f
    var nodeAnimations = arrayListOf<NodeAnimation>()

    fun read(properties: Properties) {
        nodeAnimations.clear()

        properties.getString("name")?.let { name = it }
        properties.getFloat("duration")?.let { duration = it }

        val nodeAnimationsProperties = properties.getPropertiesArray("nodeAnimations") ?: return

        for (nodeAnimationProperties in nodeAnimationsProperties) {
            val nodeAnimation = NodeAnimation()
            nodeAnimation.read(nodeAnimationProperties)
            nodeAnimations.add(nodeAnimation)
        }
    }

    fun write(properties: Properties) {
        properties.setString("name", name)
        properties.setFloat("duration", duration)

        val nodeAnimationsProperties = arrayListOf<Properties>()

        nodeAnimations.forEach {
            val nodeAnimationProperties = Properties()
            it.write(nodeAnimationProperties)
            nodeAnimationsProperties += nodeAnimationProperties
        }

        properties.setPropertiesArray("nodeAnimations", nodeAnimationsProperties.toTypedArray())
    }
}