package engine.input

import com.cozmicgames.Kore
import com.cozmicgames.input
import com.cozmicgames.input.GamepadButton
import com.cozmicgames.input.InputListener
import com.cozmicgames.input.Key
import com.cozmicgames.input.MouseButton
import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.Properties
import com.cozmicgames.utils.Reflection
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

class ControlAction(var name: String) : Disposable {
    var deadZone = 0.01f
    var rampUpSpeed = Float.MAX_VALUE
    var rampDownSpeed = Float.MAX_VALUE
    var receiveInputFromAllGamepads = true
    val gamepads = hashSetOf<Int>()

    val state get() = abs(currentValueRaw) > deadZone

    var currentValue = 0.0f
        private set

    var currentValueRaw = 0.0f
        private set

    var isTriggered = false
        private set

    private val inputs = hashMapOf<KClass<*>, ControlInput>()

    fun update(delta: Float) {
        inputs.forEach { (_, input) ->
            input.update(this)
        }

        isTriggered = inputs.any { (_, input) -> input.isTriggered }
        currentValueRaw = inputs.maxOf { (_, input) -> input.currentValue }

        if (currentValue < 0.0f) {
            if (abs(currentValueRaw) <= deadZone) {
                currentValue += rampDownSpeed * delta
                currentValue = min(0.0f, currentValue)
            } else {
                currentValue -= rampUpSpeed * delta
                currentValue = max(currentValueRaw, currentValue)
            }
        } else {
            if (abs(currentValueRaw) <= deadZone) {
                currentValue -= rampDownSpeed * delta
                currentValue = max(0.0f, currentValue)
            } else {
                currentValue += rampUpSpeed * delta
                currentValue = min(currentValueRaw, currentValue)
            }
        }
    }

    fun write(properties: Properties) {
        properties.setFloat("rampUpSpeed", rampUpSpeed)
        properties.setFloat("rampDownSpeed", rampDownSpeed)
        properties.setFloat("deadZone", deadZone)
        properties.setBoolean("receiveInputFromAllGamepads", receiveInputFromAllGamepads)
        properties.setIntArray("gamepads", gamepads.toTypedArray())

        val inputsProperties = arrayListOf<Properties>()
        inputs.forEach { (type, input) ->
            val inputProperties = Properties()
            val typeName = Reflection.getClassName(type)
            inputProperties.setString("type", typeName)
            input.write(inputProperties)
            inputsProperties += inputProperties
        }

        properties.setPropertiesArray("inputs", inputsProperties.toTypedArray())
    }

    fun read(properties: Properties) {
        rampUpSpeed = properties.getFloat("rampUpSpeed") ?: Float.MAX_VALUE
        rampDownSpeed = properties.getFloat("rampDownSpeed") ?: Float.MAX_VALUE
        deadZone = properties.getFloat("deadZone") ?: 0.01f
        receiveInputFromAllGamepads = properties.getBoolean("receiveInputFromAllGamepads") ?: true

        properties.getIntArray("gamepads")?.let {
            gamepads.clear()
            gamepads.addAll(it)
        }

        properties.getPropertiesArray("inputs")?.let {
            for (inputProperties in it) {
                val typeName = inputProperties.getString("type") ?: continue
                val type = Reflection.getClassByName(typeName) ?: continue
                val input = inputs.getOrPut(type) { Reflection.createInstance(type) as ControlInput }
                input.read(inputProperties)
            }
        }
    }

    fun addKey(key: Key) {
        val input = inputs.getOrPut(KeyControlInput::class) { KeyControlInput() } as KeyControlInput

        input.keys += key
    }

    fun removeKey(key: Key) {
        val input = (inputs[KeyControlInput::class] as? KeyControlInput) ?: return
        input.keys -= key

        if (input.keys.isEmpty())
            inputs.remove(KeyControlInput::class)
    }

    fun clearKeys() {
        inputs.remove(KeyControlInput::class)
    }

    fun getKeys(): Set<Key> {
        val input = (inputs[KeyControlInput::class] as? KeyControlInput) ?: return emptySet()
        return input.keys.toSet()
    }

    fun addMouseButton(button: MouseButton) {
        val input = inputs.getOrPut(MouseButtonControlInput::class) { MouseButtonControlInput() } as MouseButtonControlInput

        input.buttons += button
    }

    fun removeMouseButton(button: MouseButton) {
        val input = (inputs[MouseButtonControlInput::class] as? MouseButtonControlInput) ?: return
        input.buttons -= button

        if (input.buttons.isEmpty())
            inputs.remove(MouseButtonControlInput::class)
    }

    fun clearMouseButtons() {
        inputs.remove(MouseButtonControlInput::class)
    }

    fun getMouseButtons(): Set<MouseButton> {
        val input = (inputs[MouseButtonControlInput::class] as? MouseButtonControlInput) ?: return emptySet()
        return input.buttons.toSet()
    }

    fun setDeltaX() {
        inputs.getOrPut(MouseDeltaXControlInput::class) { MouseDeltaXControlInput() }
    }

    fun unsetDeltaX() {
        inputs.remove(MouseDeltaXControlInput::class)
    }

    fun isDeltaX(): Boolean {
        return MouseDeltaXControlInput::class in inputs
    }

    fun setDeltaY() {
        inputs.getOrPut(MouseDeltaYControlInput::class) { MouseDeltaYControlInput() }
    }

    fun unsetDeltaY() {
        inputs.remove(MouseDeltaYControlInput::class)
    }

    fun isDeltaY(): Boolean {
        return MouseDeltaYControlInput::class in inputs
    }

    fun setScrollX() {
        inputs.getOrPut(MouseScrollXControlInput::class) { MouseScrollXControlInput() }
    }

    fun unsetScrollX() {
        inputs.remove(MouseScrollXControlInput::class)
    }

    fun isScrollX(): Boolean {
        return MouseScrollXControlInput::class in inputs
    }

    fun setScrollY() {
        inputs.getOrPut(MouseScrollYControlInput::class) { MouseScrollYControlInput() }
    }

    fun unsetScrollY() {
        inputs.remove(MouseScrollYControlInput::class)
    }

    fun isScrollY(): Boolean {
        return MouseScrollYControlInput::class in inputs
    }

    fun addGamepadButton(button: GamepadButton) {
        val input = inputs.getOrPut(GamepadButtonControlInput::class) { GamepadButtonControlInput() } as GamepadButtonControlInput

        input.buttons += button
    }

    fun removeGamepadButton(button: GamepadButton) {
        val input = (inputs[GamepadButtonControlInput::class] as? GamepadButtonControlInput) ?: return
        input.buttons -= button

        if (input.buttons.isEmpty())
            inputs.remove(GamepadButtonControlInput::class)
    }

    fun clearGamepadButtons() {
        inputs.remove(GamepadButtonControlInput::class)
    }

    fun getGamepadButtons(): Set<GamepadButton> {
        val input = (inputs[GamepadButtonControlInput::class] as? GamepadButtonControlInput) ?: return emptySet()
        return input.buttons.toSet()
    }

    fun setGamepadLeftX() {
        inputs.getOrPut(GamepadLeftXStickControlInput::class) { GamepadLeftXStickControlInput() }
    }

    fun unsetGamepadLeftX() {
        inputs.remove(GamepadLeftXStickControlInput::class)
    }

    fun isGamepadLeftX(): Boolean {
        return GamepadLeftXStickControlInput::class in inputs
    }

    fun setGamepadLeftY() {
        inputs.getOrPut(GamepadLeftYStickControlInput::class) { GamepadLeftYStickControlInput() }
    }

    fun unsetGamepadLeftY() {
        inputs.remove(GamepadLeftYStickControlInput::class)
    }

    fun isGamepadLeftY(): Boolean {
        return GamepadLeftYStickControlInput::class in inputs
    }

    fun setGamepadRightX() {
        inputs.getOrPut(GamepadRightXStickControlInput::class) { GamepadRightXStickControlInput() }
    }

    fun unsetGamepadRightX() {
        inputs.remove(GamepadRightXStickControlInput::class)
    }

    fun isGamepadRightX(): Boolean {
        return GamepadRightXStickControlInput::class in inputs
    }

    fun setGamepadRightY() {
        inputs.getOrPut(GamepadRightYStickControlInput::class) { GamepadRightYStickControlInput() }
    }

    fun unsetGamepadRightY() {
        inputs.remove(GamepadRightYStickControlInput::class)
    }

    fun isGamepadRightY(): Boolean {
        return GamepadRightYStickControlInput::class in inputs
    }

    fun setGamepadLeftTrigger() {
        inputs.getOrPut(GamepadLeftTriggerControlInput::class) { GamepadLeftTriggerControlInput() }
    }

    fun unsetGamepadLeftTrigger() {
        inputs.remove(GamepadLeftTriggerControlInput::class)
    }

    fun isGamepadLeftTrigger(): Boolean {
        return GamepadLeftTriggerControlInput::class in inputs
    }

    fun setGamepadRightTrigger() {
        inputs.getOrPut(GamepadRightTriggerControlInput::class) { GamepadRightTriggerControlInput() }
    }

    fun unsetGamepadRightTrigger() {
        inputs.remove(GamepadRightTriggerControlInput::class)
    }

    fun isGamepadRightTrigger(): Boolean {
        return GamepadRightTriggerControlInput::class in inputs
    }

    override fun dispose() {
        inputs.forEach { (_, input) ->
            (input as? Disposable)?.dispose()
        }
    }
}

fun ControlAction.addNextKey(filter: (Key) -> Boolean = { true }, cancel: () -> Boolean = { false }) {
    val listener = object : InputListener {
        override fun onKey(key: Key, down: Boolean, time: Double) {
            if (cancel()) {
                Kore.onNextFrame {
                    Kore.input.removeListener(this)
                }

                return
            }

            if (down && filter(key)) {
                addKey(key)

                Kore.onNextFrame {
                    Kore.input.removeListener(this)
                }
            }
        }
    }

    Kore.input.addListener(listener)
}

fun ControlAction.addNextMouseButton(filter: (MouseButton) -> Boolean = { true }, cancel: () -> Boolean = { false }) {
    val listener = object : InputListener {
        override fun onMouseButton(button: MouseButton, down: Boolean, time: Double) {
            if (cancel()) {
                Kore.onNextFrame {
                    Kore.input.removeListener(this)
                }

                return
            }

            if (down && filter(button)) {
                addMouseButton(button)

                Kore.onNextFrame {
                    Kore.input.removeListener(this)
                }
            }
        }
    }

    Kore.input.addListener(listener)
}
