# Device Thermal Zone Adaptation Guide

This guide explains how to add per-device thermal zone layouts for `FanControl`.

## Core Idea

`ThermalSensorReader` reads thermal zones from `/sys/class/thermal/thermal_zone*/type` and groups them into four categories: `CPU`, `GPU`, `DDR`, and `BATTERY`.

Each device exposes its own thermal zone names. The simplest and most reliable way to support a new device is to list the exact zone names for that device.

## Data Structures

### `DeviceThermalProfile`

A plain data class that lists the exact thermal zone `type` strings for one device.

```kotlin
data class DeviceThermalProfile(
    val name: String,
    val cpuZones: List<String> = emptyList(),
    val gpuZones: List<String> = emptyList(),
    val ddrZones: List<String> = emptyList(),
    val batteryZones: List<String> = emptyList(),
)
```

### `ThermalProfiles`

A registry that maps device codenames (regular expressions) to `DeviceThermalProfile` instances.

```kotlin
object ThermalProfiles {
    private val profiles = mapOf(
        "rp6" to DeviceThermalProfile(
            name = "Retroid Pocket 6 (rp6)",
            cpuZones = listOf("cpu-0-0", "cpu-0-1"),
            gpuZones = listOf("gpuss-0"),
            ddrZones = listOf("ddr"),
            batteryZones = listOf("battery"),
        ),
    )
}
```

- Map keys are **case-insensitive regular expressions** matched against `android.os.Build.DEVICE`.
- If a device has a profile, `ThermalSensorReader` uses **exact zone name matching**.
- If a device has no profile, `ThermalSensorReader` falls back to a small set of generic prefix patterns.

## How to Add a New Device

1. Get the device codename:

   ```shell
   adb shell getprop ro.product.device
   ```

2. List the thermal zones on the device:

   ```shell
   adb shell 'for z in /sys/class/thermal/thermal_zone*; do echo "$z/type -> $(cat $z/type)"; done'
   ```

   Example output:

   ```text
   /sys/class/thermal/thermal_zone0/type -> cpu-0-0
   /sys/class/thermal/thermal_zone1/type -> cpu-0-1
   /sys/class/thermal/thermal_zone2/type -> gpuss-0
   /sys/class/thermal/thermal_zone3/type -> ddr
   /sys/class/thermal/thermal_zone4/type -> battery
   ```

3. Open [`ThermalProfiles`](ThermalClassifier.kt) and add a new entry.

### Example

For a device codenamed `example`:

```kotlin
"example" to DeviceThermalProfile(
    name = "Example Device",
    cpuZones = listOf("cpu-0-0", "cpu-0-1"),
    gpuZones = listOf("gpu-0"),
    ddrZones = listOf("ddr"),
    batteryZones = listOf("battery"),
),
```

### Matching multiple variants

Use a regex key when several variants share the same thermal layout:

```kotlin
"example-(pro|plus|max)" to DeviceThermalProfile(
    name = "Example Pro/Plus/Max",
    cpuZones = listOf("cluster-0-0", "cluster-0-1"),
    gpuZones = listOf("gpu-0"),
    ddrZones = listOf("ddr"),
    batteryZones = listOf("battery"),
),
```

## Testing

Add a unit test in [`FanControlTest`](../../../../../test/java/com/mmax/fancontrol/FanControlTest.kt) for each new profile:

```kotlin
@Test
fun exampleDeviceProfile_hasExpectedZones() {
    val profile = requireNotNull(ThermalProfiles.profileFor("example"))
    assertEquals(ThermalKind.CPU, profile.kindOf("cpu-0-0"))
    assertEquals(ThermalKind.GPU, profile.kindOf("gpu-0"))
    assertEquals(ThermalKind.DDR, profile.kindOf("ddr"))
    assertEquals(ThermalKind.BATTERY, profile.kindOf("battery"))
}
```

Run tests:

```shell
./gradlew :app:testDebugUnitTest
```

## Notes

- **Exact matching**: a zone is only classified if its `type` string appears in the profile. This avoids false positives.
- **Fallback**: devices without a profile still work through generic prefix matching (`cpu-*`, `cpuss-*`, `gpuss-*`, `ddr`, `battery`). Add a profile when the generic fallback is not enough.
- **Fill in real zone names**: the `rp6` profile currently contains only placeholder comments. Replace them with the actual zone names read from a real device.
