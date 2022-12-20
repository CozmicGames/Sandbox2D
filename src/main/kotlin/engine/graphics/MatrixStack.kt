package engine.graphics

import com.cozmicgames.utils.maths.Matrix3x2

class MatrixStack {
    private val matrices = arrayListOf<Matrix3x2>()

    private val temp = Matrix3x2()

    val isEmpty get() = matrices.isEmpty()

    val isNotEmpty get() = matrices.isNotEmpty()

    val currentMatrix get() = if (matrices.size == 1) matrices.first() else temp

    fun push(matrix: Matrix3x2) {
        matrices += matrix

        if (matrices.size > 2)
            temp.mul(matrix)
        else if (matrices.size > 1) {
            temp.set(matrices.first())
            temp.mul(matrix)
        }
    }

    fun pop() {
        matrices.removeLast()
        temp.setIdentity()

        if (matrices.size > 1)
            matrices.forEach {
                temp.mul(it)
            }
    }

    fun clear() {
        matrices.clear()
        temp.setIdentity()
    }
}