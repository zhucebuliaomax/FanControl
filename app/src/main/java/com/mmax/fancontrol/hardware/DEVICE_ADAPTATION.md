# Device Thermal Zone Adaptation Guide

This guide explains how to add per-device thermal zone classification rules for `FanControl`.

## Overview

`ThermalSensorReader` discovers thermal zones under `/sys/class/thermal/thermal_zone*`. Each zone has a `type` string (e.g. `cpu-1-3`, `gpuss-7`, `battery`). The reader must map these strings to one of the four categories in [`ThermalKind`](ThermalClassifier.kt):

- `CPU`
- `GPU`
- `DDR`
- `BATTERY`

The mapping is configured through **rules** and **device profiles** so that new devices can be supported without changing the core reading logic.

## Core Components

### `ThermalClassificationRule`

A single rule that maps a thermal zone `type` string to a `ThermalKind`.

```kotlin
data class ThermalClassificationRule(
    val kind: ThermalKind,
    val pattern: String,
    val mode: ThermalMatchMode = ThermalMatchMode.PREFIX,
)
```

- `kind`: the category to assign.
- `pattern`: the value to match against the `type` string.
- `mode`: how to match. See [Match Modes](#match-modes).

Rules are evaluated in order; the **first matching rule wins**.

### `ThermalMatchMode`

| Mode | Behavior | Example pattern | Matches | Does not match |
|------|----------|-----------------|---------|----------------|
| `EXACT` | Full equality | `ddr` | `ddr` | `ddram`, `ddr0` |
| `PREFIX` | Type starts with pattern | `cpu-` | `cpu-1-3` | `cpuss-0` |
| `CONTAINS` | Type contains pattern | `cpu` | `soc-cpu-zone` | `gpu-0` |
| `REGEX` | Pattern is a case-insensitive regex | `^gpu.*` | `gpu-0`, `gpu-1` | `sgpu` |

### `DeviceThermalProfile`

A per-device configuration.

```kotlin
data class DeviceThermalProfile(
    val name: String,
    val additionalRules: List<ThermalClassificationRule> = emptyList(),
    val overrideRules: List<ThermalClassificationRule>? = null,
)
```

- `additionalRules`: appended **before** the generic rules, so they take priority.
- `overrideRules`: if set, replaces the generic rules entirely for that device.

### `ThermalClassificationProfiles`

The registry that holds all device profiles. It matches the running device's `Build.DEVICE` value against profile keys.

```kotlin
private val profiles = mapOf(
    "rp6" to DeviceThermalProfile(
        name = "Redmi Pad Pro (rp6)",
        additionalRules = emptyList(),
    ),
)
```

- Profile keys are **case-insensitive regular expressions** matched against `Build.DEVICE`.
- A device that does not match any profile uses the **generic rules** only.

## Generic Rules

Generic rules are the default classification used for any device that has no specific profile, or as the base for devices with `additionalRules`.

```kotlin
val genericRules = listOf(
    ThermalClassificationRule(ThermalKind.CPU, "cpu-"),
    ThermalClassificationRule(ThermalKind.CPU, "cpuss-"),
    ThermalClassificationRule(ThermalKind.GPU, "gpuss-"),
    ThermalClassificationRule(ThermalKind.DDR, "ddr", ThermalMatchMode.EXACT),
    ThermalClassificationRule(ThermalKind.BATTERY, "battery", ThermalMatchMode.EXACT),
)
```

## How to Add a New Device

1. Identify the device codename. On the device or emulator, run:

   ```shell
   adb shell getprop ro.product.device
   ```

2. Inspect the thermal zone types:

   ```shell
   adb shell 'for z in /sys/class/thermal/thermal_zone*; do echo "$z/type -> $(cat $z/type)"; done'
   ```

3. Decide which zone types belong to `CPU`, `GPU`, `DDR`, and `BATTERY`.

4. Open [`ThermalClassificationProfiles`](ThermalClassifier.kt) and add a new entry to the `profiles` map.

### Example: adding a device with extra sensors

Assume a device codenamed `example` reports a thermal zone type `soc-cpu-big-0` that the generic `cpu-` prefix does not match.

```kotlin
"example" to DeviceThermalProfile(
    name = "Example Device",
    additionalRules = listOf(
        ThermalClassificationRule(ThermalKind.CPU, "soc-cpu-", ThermalMatchMode.PREFIX),
    ),
),
```

This keeps all generic rules and adds the new rule at the front.

### Example: adding a device that needs different rules

Assume a device codenamed `custom` uses `processor-cpu` and `processor-gpu` instead of the generic names.

```kotlin
"custom" to DeviceThermalProfile(
    name = "Custom Device",
    overrideRules = listOf(
        ThermalClassificationRule(ThermalKind.CPU, "processor-cpu", ThermalMatchMode.CONTAINS),
        ThermalClassificationRule(ThermalKind.GPU, "processor-gpu", ThermalMatchMode.CONTAINS),
        ThermalClassificationRule(ThermalKind.DDR, "ddr", ThermalMatchMode.EXACT),
        ThermalClassificationRule(ThermalKind.BATTERY, "battery", ThermalMatchMode.EXACT),
    ),
),
```

### Example: matching multiple device variants with one regex

Use a regex key when several variants share the same thermal naming.

```kotlin
"custom-(pro|plus|max)" to DeviceThermalProfile(
    name = "Custom Pro/Plus/Max series",
    additionalRules = listOf(
        ThermalClassificationRule(ThermalKind.CPU, "cluster-0-", ThermalMatchMode.PREFIX),
    ),
),
```

## Testing

Add unit tests in [`FanControlTest`](../../../../../test/java/com/mmax/fancontrol/FanControlTest.kt). Verify both the generic rules and the device-specific rules.

```kotlin
@Test
fun profileCustom_cpuZoneIsClassified() {
    val classifier = ThermalClassificationProfiles.classifierFor("custom-pro")
    assertEquals(ThermalKind.CPU, classifier.classify("processor-cpu-0"))
    assertEquals(ThermalKind.GPU, classifier.classify("processor-gpu-0"))
}
```

Run tests before building:

```shell
./gradlew :app:testDebugUnitTest
```

## Best Practices

- **Prefer `additionalRules` over `overrideRules`**: only replace the generic rules when the device truly uses incompatible naming.
- **Keep patterns specific**: a very broad `CONTAINS` or `REGEX` pattern can misclassify unrelated zones.
- **Order matters**: place the most specific rules first.
- **Document the device**: use a clear `name` in the profile.
- **Add a test for every new device profile** to prevent regressions.
