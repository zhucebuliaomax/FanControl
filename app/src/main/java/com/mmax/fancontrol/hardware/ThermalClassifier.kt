package com.mmax.fancontrol.hardware

import android.os.Build

// Per-device thermal zone configuration.
// List the exact `type` strings reported by each thermal zone.
data class DeviceThermalProfile(
    val name: String,
    val cpuZones: List<String> = emptyList(),
    val gpuZones: List<String> = emptyList(),
    val ddrZones: List<String> = emptyList(),
    val batteryZones: List<String> = emptyList(),
) {
    fun kindOf(type: String): ThermalKind? = when (type) {
        in cpuZones -> ThermalKind.CPU
        in gpuZones -> ThermalKind.GPU
        in ddrZones -> ThermalKind.DDR
        in batteryZones -> ThermalKind.BATTERY
        else -> null
    }
}

// Registry of known device thermal zone layouts.
// Keys are case-insensitive regular expressions matched against Build.DEVICE.
object ThermalProfiles {

    private val profiles = mapOf(
        "rp6" to DeviceThermalProfile(
            name = "Retroid Pocket 6 (rp6)",
        ),
    )

    fun profileFor(device: String): DeviceThermalProfile? = profiles.entries
        .find { device.matches(Regex(it.key, RegexOption.IGNORE_CASE)) }
        ?.value
}
