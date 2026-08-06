package com.mmax.retrocontrol.hardware

import com.topjohnwu.superuser.Shell

/** Root-backed writer for the eight RP6 joystick RGB LEDs. */
object JoystickRgbController {
    val ledPaths: List<String> = listOf(
        "/sys/class/leds/left:stick:0",
        "/sys/class/leds/left:stick:1",
        "/sys/class/leds/left:stick:2",
        "/sys/class/leds/left:stick:3",
        "/sys/class/leds/right:stick:0",
        "/sys/class/leds/right:stick:1",
        "/sys/class/leds/right:stick:2",
        "/sys/class/leds/right:stick:3",
    )

    fun execute(script: String) {
        if (script.isBlank()) return
        Shell.cmd(script).exec()
    }

    fun setAll(red: Int, green: Int, blue: Int, brightness: Int) {
        val color = "${red.coerceIn(0, 255)} ${green.coerceIn(0, 255)} ${blue.coerceIn(0, 255)}"
        val level = brightness.coerceIn(0, 255)
        execute(buildString(512) {
            ledPaths.forEach { path ->
                append("echo \"").append(color).append("\" > ")
                    .append(path).append("/multi_intensity\n")
                append("echo ").append(level).append(" > ")
                    .append(path).append("/brightness\n")
            }
        })
    }

    fun setBrightness(brightness: Int) {
        val level = brightness.coerceIn(0, 255)
        execute(buildString(256) {
            ledPaths.forEach { path ->
                append("echo ").append(level).append(" > ")
                    .append(path).append("/brightness\n")
            }
        })
    }

    fun turnOff() = setBrightness(0)
}
