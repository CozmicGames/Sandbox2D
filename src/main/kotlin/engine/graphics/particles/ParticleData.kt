package engine.graphics.particles

import com.cozmicgames.utils.extensions.swap
import kotlin.reflect.KClass

class ParticleData(val maxParticles: Int) {
    interface DataType

    val arrays = hashMapOf<KClass<*>, Array<*>>()

    var numberOfAlive = 0

    inline fun <reified T : DataType> getArray(initialValue: () -> T) = arrays.getOrPut(T::class) { Array(maxParticles) { initialValue() } } as Array<T>

    inline fun <reified T : DataType> getArrayOrNull() = arrays[T::class] as? Array<T>?

    fun kill(id: Int) {
        if (numberOfAlive <= 0)
            return

        arrays.forEach { (_, array) ->
            (array as Array<Any>).swap(id, numberOfAlive - 1)
        }

        numberOfAlive--
    }
}