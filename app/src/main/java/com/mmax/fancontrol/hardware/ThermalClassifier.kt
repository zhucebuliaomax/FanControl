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
            cpuZones = listOf(
                "cpu-0-0", "cpu-0-1", "cpu-0-2",
                "cpu-1-0", "cpu-1-1", "cpu-1-2", "cpu-1-3", "cpu-1-4",
                "cpu-1-5", "cpu-1-6", "cpu-1-7", "cpu-1-8", "cpu-1-9", "cpu-1-10",
            ),
            gpuZones = listOf(
                "gpuss-0", "gpuss-1", "gpuss-2", "gpuss-3",
                "gpuss-4", "gpuss-5", "gpuss-6", "gpuss-7",
            ),
            ddrZones = listOf("ddr"),
            batteryZones = listOf("battery"),
        ),
    )

    fun profileFor(device: String): DeviceThermalProfile? = profiles.entries
        .find { device.matches(Regex(it.key, RegexOption.IGNORE_CASE)) }
        ?.value
}
