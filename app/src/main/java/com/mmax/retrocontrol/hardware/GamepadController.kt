package com.mmax.retrocontrol.hardware

import com.mmax.retrocontrol.data.ButtonLayoutProfile
import com.mmax.retrocontrol.data.FaceButtonLayout
import com.mmax.retrocontrol.data.GamepadButtonMapping
import com.mmax.retrocontrol.data.GamepadTriggerMode
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Root-backed access to the RP6 Moorechip gamepad configuration nodes. */
object GamepadController {
    internal const val LAYOUT_PATH = "/sys/class/moorechip-joystick/joystick/layout"
    internal const val M1_PATH = "/sys/class/moorechip-joystick/joystick/m0_function"
    internal const val M2_PATH = "/sys/class/moorechip-joystick/joystick/m1_function"
    internal const val TRIGGER_PATH = "/sys/class/moorechip-joystick/joystick/triggers"

    private val applyMutex = Mutex()

    data class State(
        val layout: FaceButtonLayout,
        val m1: GamepadButtonMapping,
        val m2: GamepadButtonMapping,
        val triggerMode: GamepadTriggerMode,
    )

    suspend fun applyProfile(profile: ButtonLayoutProfile): Result<State> = runCatching {
        applyMutex.withLock {
            val target = profile.normalized().let {
                State(it.layout, it.m1, it.m2, it.triggerMode)
            }
            val current = readState().getOrThrow()
            val script = buildApplyScript(current, target)
            if (script.isNotBlank()) {
                val result = Shell.cmd(script).exec()
                check(result.isSuccess) {
                    result.err.joinToString().ifBlank { "Unable to write gamepad settings" }
                }
            }
            val actual = readState().getOrThrow()
            check(actual == target) {
                "Gamepad setting verification failed: requested=$target actual=$actual"
            }
            actual
        }
    }

    fun readState(): Result<State> = runCatching {
        val result = Shell.cmd(
            "cat $LAYOUT_PATH 2>/dev/null; " +
                "cat $M1_PATH 2>/dev/null; " +
                "cat $M2_PATH 2>/dev/null; " +
                "cat $TRIGGER_PATH 2>/dev/null",
        ).exec()
        check(result.isSuccess) {
            result.err.joinToString().ifBlank { "Unable to read gamepad settings" }
        }
        parseState(result.out)
    }

    internal fun parseState(lines: List<String>): State {
        require(lines.size >= 4) { "Incomplete gamepad state" }
        val values = lines.takeLast(4).map(String::trim)
        return State(
            layout = FaceButtonLayout.entries.firstOrNull { it.sysfsValue == values[0] }
                ?: error("Unknown gamepad layout: ${values[0]}"),
            m1 = GamepadButtonMapping.entries.firstOrNull { it.sysfsValue == values[1] }
                ?: error("Unknown M1 mapping: ${values[1]}"),
            m2 = GamepadButtonMapping.entries.firstOrNull { it.sysfsValue == values[2] }
                ?: error("Unknown M2 mapping: ${values[2]}"),
            triggerMode = GamepadTriggerMode.entries.firstOrNull {
                it.sysfsValue == values[3]
            } ?: error("Unknown trigger mode: ${values[3]}"),
        )
    }

    /** Back buttons are written first; trigger/layout changes re-register the input device. */
    internal fun buildApplyScript(current: State, target: State): String = buildString {
        if (current.m1 != target.m1) appendLine("echo ${target.m1.sysfsValue} > $M1_PATH")
        if (current.m2 != target.m2) appendLine("echo ${target.m2.sysfsValue} > $M2_PATH")
        if (current.triggerMode != target.triggerMode) {
            appendLine("echo ${target.triggerMode.sysfsValue} > $TRIGGER_PATH")
        }
        if (current.layout != target.layout) {
            appendLine("echo ${target.layout.sysfsValue} > $LAYOUT_PATH")
        }
    }.trim()
}
