# Project Structure

## Purpose

RetroControl is a fan-control application for rooted Android handhelds. It reads CPU, GPU, memory, and battery temperatures from sysfs, calculates a target speed from the fan curve selected by the user, and writes the resulting output through a `pwm-fan` PWM node or thermal cooling device.

The application does not disable kernel thermal control, change CPU or GPU limits, or modify thermal-zone modes. Hardware writes are isolated in the fan-control layer so that they remain easy to review and adapt.

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

The main source code is under `app/src/main/java/com/mmax/fancontrol/`.

| Package | Primary responsibility |
| --- | --- |
| `data` | Fan-curve models, JSON import/export, SharedPreferences persistence, and legacy-data migration |
| `hardware` | Thermal-sensor classification and reading, fan-node discovery and writing, temperature filtering, and speed ramping |
| `service` | Foreground service, control loop, notification, screen-off suspension, and curve adjustment |
| `ui` | Main settings screen, curve editor, and ViewModel |
| `overlay` | Floating telemetry window and its Compose lifecycle host |
| `tile` | Fan and overlay Quick Settings tiles and their routing activities |
| `theme` | Material theme, colors, and typography |
| `util` | Temperature and fan-speed display formatting |

Application entry points and root-shell management live at the package root:

- `FanControlApp.kt`: Application initialization.
- `MainActivity.kt`: Main UI entry point.
- `RootAccessManager.kt`: Serializes root-shell acquisition.

## Core data flow

1. `ThermalSensorReader` scans thermal zones. `ThermalClassifier` retains only sensors that clearly represent the CPU, GPU, DDR/DRAM, or battery.
2. `ThermalSnapshot` summarizes each category and uses the higher of the CPU and GPU average temperatures as the control temperature.
3. `SystemControlService` reads the active curve and interpolates the control temperature into a target fan percentage.
4. `FanResponseController` applies window filtering, output deadband evaluation, and ramping.
5. `FanController` writes to `pwm1` when available and falls back to `cooling_device/cur_state` otherwise.
6. `TelemetryRepository` provides the same runtime state to the dashboard, notification, and overlay.

## Resources and tests

- Localized strings live in each module's `src/main/res/values*` directories.
- Unit tests live in `app/src/test/java/com/mmax/fancontrol/FanControlTest.kt`.
- Device inspection and adaptation instructions are in `docs/FAN_ADAPTATION.md`.
- Common verification commands:

```shell
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```
