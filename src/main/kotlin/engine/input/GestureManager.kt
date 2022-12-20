package engine.input

import com.cozmicgames.Kore
import com.cozmicgames.input
import com.cozmicgames.input.InputListener
import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.Time
import com.cozmicgames.utils.Updateable
import com.cozmicgames.utils.maths.Rectangle
import com.cozmicgames.utils.maths.Vector2

class GestureManager : Updateable, InputListener, Disposable {
    var tapRectangleSize = 20.0f
        set(value) {
            field = value
            tapRectangle.width = value
            tapRectangle.height = value
        }

    var tapInterval = 0.4f

    var longPressDuration = 1.1f

    private val listeners = arrayListOf<GestureListener>()
    private val tapRectangle = Rectangle(0.0f, 0.0f, tapRectangleSize, tapRectangleSize)
    private var isInTapRectangle = false
    private var tapCount = 0
    private var lastTapCount = 0
    private var lastTapTime = 0.0
    private var isTouched = false
    private var longPressCounter = 0.0
    private var hasLongPressFired = false
    private val pointer = Vector2()
    private val lastTapPointer = Vector2()


    init {
        Kore.input.addListener(this)
    }

    fun addListener(listener: GestureListener) {
        listeners += listener
    }

    fun removeListener(listener: GestureListener) {
        listeners -= listener
    }

    override fun onTouch(x: Int, y: Int, pointer: Int, down: Boolean, time: Double) {
        isTouched = down

        this.pointer.set(x.toFloat(), y.toFloat())

        if (down) {
            isInTapRectangle = true
            tapRectangle.centerX = x.toFloat()
            tapRectangle.centerY = y.toFloat()
        } else {
            if (isInTapRectangle && this.pointer !in tapRectangle)
                isInTapRectangle = false

            if (!hasLongPressFired) {
                if (isInTapRectangle) {
                    if (this.lastTapPointer !in tapRectangle || Time.current - lastTapTime > tapInterval)
                        tapCount = 0

                    tapCount++
                    lastTapTime = Time.current
                    lastTapPointer.set(x.toFloat(), y.toFloat())

                    listeners.forEach {
                        it.onTap(tapRectangle.centerX, tapRectangle.centerY, tapCount)
                    }
                }
            }
        }
    }

    override fun update(delta: Float) {
        if (isTouched)
            longPressCounter += delta
        else {
            longPressCounter = 0.0
            hasLongPressFired = false
        }

        if (longPressCounter >= longPressDuration && !hasLongPressFired) {
            hasLongPressFired = true

            listeners.forEach {
                it.onLongPress(pointer.x, pointer.y)
            }
        }
    }

    override fun dispose() {
        Kore.input.removeListener(this)
    }
}
