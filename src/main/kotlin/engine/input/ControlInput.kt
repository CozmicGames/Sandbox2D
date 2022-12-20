package engine.input

import com.cozmicgames.Kore
import com.cozmicgames.input
import com.cozmicgames.input.*
import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.Properties
import com.cozmicgames.utils.extensions.enumValueOfOrNull

interface ControlInput {
    val isTriggered: Boolean
    val currentValue: Float

    fun update(action: ControlAction) {}

    fun read(properties: Properties) {}
    fun write(properties: Properties) {}
}

class KeyControlInput : ControlInput {
    val keys = hashSetOf<Key>()

    override var isTriggered = false
        private set

    override var currentValue = 0.0f
        private set

    override fun update(action: ControlAction) {
        isTriggered = keys.any { Kore.input.isKeyJustDown(it) }
        currentValue = if (keys.any { Kore.input.isKeyDown(it) }) 1.0f else 0.0f
    }

    override fun write(properties: Properties) {
        properties.setStringArray("keys", keys.map { it.toString() }.toTypedArray())
    }

    override fun read(properties: Properties) {
        properties.getStringArray("keys")?.let {
            keys.clear()

            for (keyName in it) {
                val key = enumValueOfOrNull<Keys>(keyName) ?: continue
                keys += key
            }
        }
    }
}

class MouseButtonControlInput : ControlInput {
    val buttons = hashSetOf<MouseButton>()

    override var isTriggered = false
        private set

    override var currentValue = 0.0f
        private set

    override fun update(action: ControlAction) {
        isTriggered = buttons.any { Kore.input.isButtonJustDown(it) }
        currentValue = if (buttons.any { Kore.input.isButtonDown(it) }) 1.0f else 0.0f
    }

    override fun write(properties: Properties) {
        properties.setStringArray("buttons", buttons.map { it.toString() }.toTypedArray())
    }

    override fun read(properties: Properties) {
        properties.getStringArray("buttons")?.let {
            buttons.clear()

            for (buttonName in it) {
                val button = enumValueOfOrNull<MouseButtons>(buttonName) ?: continue
                buttons += button
            }
        }
    }
}

class MouseDeltaXControlInput : ControlInput {
    override var isTriggered = false
        private set

    override var currentValue = 0.0f
        private set

    override fun update(action: ControlAction) {
        isTriggered = Kore.input.deltaX != 0
        currentValue = Kore.input.deltaX.toFloat()
    }
}

class MouseDeltaYControlInput : ControlInput {
    override var isTriggered = false
        private set

    override var currentValue = 0.0f
        private set

    override fun update(action: ControlAction) {
        isTriggered = Kore.input.deltaY != 0
        currentValue = Kore.input.deltaY.toFloat()
    }
}

class MouseScrollXControlInput : ControlInput, InputListener, Disposable {
    override var isTriggered = false
        private set

    override var currentValue = 0.0f
        private set

    private var currentScrollAmount = 0.0f
    private var unsetIsTriggeredNextUpdate = false

    init {
        Kore.input.addListener(this)
    }

    override fun update(action: ControlAction) {
        currentValue = currentScrollAmount
        currentScrollAmount = 0.0f

        if (isTriggered)
            unsetIsTriggeredNextUpdate = true

        if (unsetIsTriggeredNextUpdate) {
            isTriggered = false
            unsetIsTriggeredNextUpdate = false
        }
    }

    override fun onScroll(x: Float, y: Float, time: Double) {
        isTriggered = true
        currentScrollAmount += x
    }

    override fun dispose() {
        Kore.input.removeListener(this)
    }
}

class MouseScrollYControlInput : ControlInput, InputListener, Disposable {
    override var isTriggered = false
        private set

    override var currentValue = 0.0f
        private set

    private var currentScrollAmount = 0.0f
    private var unsetIsTriggeredNextUpdate = false

    init {
        Kore.input.addListener(this)
    }

    override fun update(action: ControlAction) {
        currentValue = currentScrollAmount
        currentScrollAmount = 0.0f

        if (isTriggered)
            unsetIsTriggeredNextUpdate = true

        if (unsetIsTriggeredNextUpdate) {
            isTriggered = false
            unsetIsTriggeredNextUpdate = false
        }
    }

    override fun onScroll(x: Float, y: Float, time: Double) {
        isTriggered = true
        currentScrollAmount += y
    }

    override fun dispose() {
        Kore.input.removeListener(this)
    }
}

class GamepadButtonControlInput : ControlInput {
    val buttons = hashSetOf<GamepadButton>()

    override var isTriggered = false
        private set

    override var currentValue = 0.0f
        private set

    override fun update(action: ControlAction) {
        val gamepads = if (action.receiveInputFromAllGamepads) {
            Kore.input.gamepads
        } else
            Kore.input.gamepads.filter { it.id in action.gamepads }

        isTriggered = gamepads.any { gamepad -> buttons.any { gamepad.isButtonJustDown(it) } }
        currentValue = if (gamepads.any { gamepad -> buttons.any { gamepad.isButtonDown(it) } }) 1.0f else 0.0f
    }

    override fun write(properties: Properties) {
        properties.setStringArray("buttons", buttons.map { it.toString() }.toTypedArray())
    }

    override fun read(properties: Properties) {
        properties.getStringArray("buttons")?.let {
            buttons.clear()

            for (buttonName in it) {
                val button = enumValueOfOrNull<GamepadButtons>(buttonName) ?: continue
                buttons += button
            }
        }
    }
}

class GamepadLeftXStickControlInput : ControlInput {
    override var isTriggered = false
        private set

    override var currentValue = 0.0f
        private set

    override fun update(action: ControlAction) {
        val gamepads = if (action.receiveInputFromAllGamepads) {
            Kore.input.gamepads
        } else
            Kore.input.gamepads.filter { it.id in action.gamepads }

        isTriggered = gamepads.any {
            it.leftStick.last.x <= action.deadZone && it.leftStick.current.x > action.deadZone
        }

        currentValue = gamepads.maxOf { it.leftStick.current.x }
    }
}

class GamepadLeftYStickControlInput : ControlInput {
    override var isTriggered = false
        private set

    override var currentValue = 0.0f
        private set

    override fun update(action: ControlAction) {
        val gamepads = if (action.receiveInputFromAllGamepads) {
            Kore.input.gamepads
        } else
            Kore.input.gamepads.filter { it.id in action.gamepads }

        isTriggered = gamepads.any {
            it.leftStick.last.y <= action.deadZone && it.leftStick.current.y > action.deadZone
        }

        currentValue = gamepads.maxOf { it.leftStick.current.y }
    }
}

class GamepadRightXStickControlInput : ControlInput {
    override var isTriggered = false
        private set

    override var currentValue = 0.0f
        private set

    override fun update(action: ControlAction) {
        val gamepads = if (action.receiveInputFromAllGamepads) {
            Kore.input.gamepads
        } else
            Kore.input.gamepads.filter { it.id in action.gamepads }

        isTriggered = gamepads.any {
            it.rightStick.last.x <= action.deadZone && it.rightStick.current.x > action.deadZone
        }

        currentValue = gamepads.maxOf { it.rightStick.current.x }
    }
}

class GamepadRightYStickControlInput : ControlInput {
    override var isTriggered = false
        private set

    override var currentValue = 0.0f
        private set

    override fun update(action: ControlAction) {
        val gamepads = if (action.receiveInputFromAllGamepads) {
            Kore.input.gamepads
        } else
            Kore.input.gamepads.filter { it.id in action.gamepads }

        isTriggered = gamepads.any {
            it.rightStick.last.y <= action.deadZone && it.rightStick.current.y > action.deadZone
        }

        currentValue = gamepads.maxOf { it.rightStick.current.y }
    }
}

class GamepadLeftTriggerControlInput : ControlInput {
    override var isTriggered = false
        private set

    override var currentValue = 0.0f
        private set

    override fun update(action: ControlAction) {
        val gamepads = if (action.receiveInputFromAllGamepads) {
            Kore.input.gamepads
        } else
            Kore.input.gamepads.filter { it.id in action.gamepads }

        isTriggered = gamepads.any {
            it.leftTrigger.last <= action.deadZone && it.leftTrigger.current > action.deadZone
        }

        currentValue = gamepads.maxOf { it.leftTrigger.current }
    }
}

class GamepadRightTriggerControlInput : ControlInput {
    override var isTriggered = false
        private set

    override var currentValue = 0.0f
        private set

    override fun update(action: ControlAction) {
        val gamepads = if (action.receiveInputFromAllGamepads) {
            Kore.input.gamepads
        } else
            Kore.input.gamepads.filter { it.id in action.gamepads }

        isTriggered = gamepads.any {
            it.rightTrigger.last <= action.deadZone && it.rightTrigger.current > action.deadZone
        }

        currentValue = gamepads.maxOf { it.rightTrigger.current }
    }
}
