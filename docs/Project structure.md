# Project Structure

## Purpose

RetroControl is a root control application for Android handhelds. It reads CPU, GPU, memory, and battery temperatures from sysfs, calculates a target speed from the fan curve selected by the user, and writes the resulting output through a `pwm-fan` PWM node or thermal cooling device. Performance profiles independently cap each detected CPU cpufreq policy through `scaling_max_freq`. On RP6, named button-layout profiles also manage the face-button layout, M1/M2 mappings, and trigger mode.

The application does not disable kernel thermal control, change governors or GPU limits, or modify thermal-zone modes. CPU writes are limited to cpufreq minimum/maximum nodes and hardware writes remain isolated from Compose UI so that they are easy to review and adapt.

## Root directories

| Path | Responsibility |
| --- | --- |
| `app/` | Main Android application: state, persistence, hardware access, background service, dashboard, overlay, and Quick Settings tiles |
| `core/designsystem/` | Reusable Compose settings components and layout conventions |
| `feature/authorization/` | Root, notification, and related authorization UI |
| `feature/fan/` | Reusable fan-configuration and temperature-information UI |
| `docs/` | Project structure, extension points, and device-adaptation documentation |
| `assets/` | Reserved for images, screenshots, and release material; currently empty |
| `style/` | Machine-readable design reuse manifest |
| `gradle/` | Gradle Wrapper and centralized version catalog |
| `README.md` | User-facing project overview, control strategy, and build commands |

## Main application layers

The main source code is under `app/src/main/java/com/mmax/retrocontrol/`.

| Package | Primary responsibility |
| --- | --- |
| `data` | Fan, preset, app, joystick, button-layout, and performance-profile models plus SharedPreferences persistence and migration |
| `hardware` | Thermal reading, fan/gamepad control, CPU cpufreq policy discovery/writes, filtering, and speed ramping |
| `service` | Foreground service, app-aware control application, notification, and screen-off behavior |
| `ui` | Main settings screen, profile editors, and ViewModel |
| `overlay` | Floating telemetry window and its Compose lifecycle host |
| `tile` | Fan, overlay, joystick, performance, and button-layout Quick Settings tiles plus their routing activities |
| `theme` | Material theme, colors, and typography |
| `util` | Temperature and fan-speed display formatting |

Application entry points and root-shell management live at the package root:

- `RetroControlApp.kt`: Application initialization.
- `MainActivity.kt`: Main UI entry point.
- `RootAccessManager.kt`: Serializes root-shell acquisition.

## Core data flow

1. `ThermalSensorReader` scans thermal zones. `ThermalClassifier` retains only sensors that clearly represent the CPU, GPU, DDR/DRAM, or battery.
2. `ThermalSnapshot` summarizes each category and uses the higher of the CPU and GPU average temperatures as the control temperature.
3. `SystemControlService` reads the active curve and interpolates the control temperature into a target fan percentage.
4. `FanResponseController` applies window filtering, output deadband evaluation, and ramping.
5. `FanController` writes to `pwm1` when available and falls back to `cooling_device/cur_state` otherwise.
6. `TelemetryRepository` provides the same runtime state to the dashboard, notification, and overlay.

Performance control follows a separate event-driven path: `CpuFrequencyController` discovers cpufreq policies through the persistent root shell, `PerformanceProfileResolver` resolves preset and app overrides, and `SystemControlService` writes a new maximum only when the effective profile changes. Selecting Unmanaged after an applied profile restores the detected stock limits.

RP6 button-layout control is also event-driven. `ButtonLayoutProfilePreferences` resolves app and preset references, while `GamepadController` performs serialized, difference-only sysfs writes and read-back verification. See [`Button layout.md`](Button%20layout.md) for the device ABI and lifecycle details.

## Resources and tests

- Localized strings live in each module's `src/main/res/values*` directories.
- Unit tests live in `app/src/test/java/com/mmax/retrocontrol/`.
- Device inspection and adaptation instructions are in `docs/FAN_ADAPTATION.md`.
- Common verification commands:

```shell
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```
