package engine.scene.components

import com.cozmicgames.utils.Properties
import com.cozmicgames.utils.UUID
import com.cozmicgames.utils.Updateable
import com.cozmicgames.utils.maths.lerp
import engine.scene.Component
import engine.scene.animation.Animation
import engine.scene.animation.AnimationFrame

class AnimatorComponent : Component(), Updateable {
    private inner class RunningAnimation(val animation: Animation, val looping: Boolean) {
        var time = 0.0f
        var isFinished = false

        private val startFrames = hashMapOf<UUID, AnimationFrame>()

        fun update(delta: Float): Boolean {
            if (time == 0.0f) {
                for (nodeAnimation in animation.nodeAnimations) {
                    val nodeUUID = nodeAnimation.uuid ?: continue
                    val nodeGameObject = gameObject.scene.getGameObject(nodeUUID) ?: continue
                    val nodeTransform = nodeGameObject.getComponent<TransformComponent>()?.transform ?: continue

                    startFrames[nodeUUID] = AnimationFrame().also {
                        it.x = nodeTransform.x
                        it.y = nodeTransform.y
                        it.rotation = nodeTransform.rotation
                        it.scaleX = nodeTransform.scaleX
                        it.scaleY = nodeTransform.scaleY
                    }
                }
            }

            time += delta

            if (time >= animation.duration) {
                if (looping)
                    time -= animation.duration
                else
                    isFinished = true

                return true
            }

            for (nodeAnimation in animation.nodeAnimations) {
                if (nodeAnimation.uuid == null)
                    continue

                val nodeGameObject = gameObject.scene.getGameObject(requireNotNull(nodeAnimation.uuid)) ?: continue
                val nodeTransform = nodeGameObject.getComponent<TransformComponent>()?.transform ?: continue

                val frameTimeA: Float
                val frameTimeB: Float

                var frameA = nodeAnimation.getFrameBefore(time)
                var frameB = nodeAnimation.getFrameAfter(time)

                if (frameA == null) {
                    frameA = startFrames[nodeAnimation.uuid] ?: continue
                    frameTimeA = 0.0f
                } else
                    frameTimeA = frameA.time

                if (frameB == null) {
                    frameB = startFrames[nodeAnimation.uuid] ?: continue
                    frameTimeB = animation.duration
                } else
                    frameTimeB = frameB.time

                val frameTime = (time - frameTimeA) / (frameTimeB - frameTimeA)

                nodeTransform.x = lerp(frameA.x, frameB.x, frameTime)
                nodeTransform.y = lerp(frameA.y, frameB.y, frameTime)
                nodeTransform.rotation = lerp(frameA.rotation, frameB.rotation, frameTime)
                nodeTransform.scaleX = lerp(frameA.scaleX, frameB.scaleX, frameTime)
                nodeTransform.scaleY = lerp(frameA.scaleY, frameB.scaleY, frameTime)
            }

            return false
        }
    }

    val animations = ArrayList<Animation>()
    var defaultAnimation = "default"

    private val runningAnimations = arrayListOf<RunningAnimation>()

    fun startAnimation(name: String, looping: Boolean = false) {
        val animation = animations.firstOrNull { it.name == name } ?: return
        runningAnimations.add(RunningAnimation(animation, looping))
    }

    fun stopAnimation(name: String) {
        runningAnimations.removeAll { it.animation.name == name }
    }

    fun stopAllAnimations() {
        runningAnimations.clear()
    }

    override fun update(delta: Float) {
        val iter = runningAnimations.iterator()
        while (iter.hasNext()) {
            val animation = iter.next()
            if (animation.update(delta))
                iter.remove()
        }
    }

    override fun onAdded() {
        startAnimation(defaultAnimation, true)
    }

    override fun onActiveChanged() {
        if (gameObject.isActive)
            startAnimation(defaultAnimation, true)
        else
            stopAllAnimations()
    }

    override fun onRemoved() {
        stopAllAnimations()
    }

    override fun read(properties: Properties) {
        animations.clear()

        properties.getString("defaultAnimation")?.let { defaultAnimation = it }
        properties.getPropertiesArray("animations")?.let {
            for (animationProperties in it) {
                val animation = Animation()
                animation.read(animationProperties)
                animations.add(animation)
            }
        }
    }

    override fun write(properties: Properties) {
        properties.setString("defaultAnimation", defaultAnimation)

        val animationsProperties = arrayListOf<Properties>()
        animations.forEach {
            val animationProperties = Properties()
            it.write(animationProperties)
            animationsProperties.add(animationProperties)
        }

        properties.setPropertiesArray("animations", animationsProperties.toTypedArray())
    }
}