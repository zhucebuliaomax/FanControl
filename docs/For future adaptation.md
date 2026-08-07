# Extending the Project

## Before making changes

First determine whether a change belongs to the UI, business state, hardware adaptation, or background control. Preserve the existing separation of concerns: Compose UI must not access sysfs directly, and device-specific checks must not be scattered through services or screens.

Run the following commands before making changes:

```shell
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Run the same commands afterward so that pre-existing failures can be distinguished from regressions introduced by the change.

## Extending the UI

- Place shared settings rows and grouping visuals in `core/designsystem`.
- Place independently reusable fan and authorization cards in `feature/fan` and `feature/authorization` respectively.
- Place composition logic that requires main-application state, dialogs, or navigation in `app/ui`.
- Put user-visible text in the corresponding module's `res/values/strings.xml` and update the existing localized resources.
- A major feature may have a short KDoc above its public entry point describing its responsibility, inputs, outputs, and important boundaries. Avoid recording debugging history or narrating the implementation line by line.

## Extending fan curves

- Data structures and built-in curves: `data/FanCurveData.kt`.
- JSON format: `data/FanCurveJson.kt`.
- Saving, selection, migration, and reset behavior: `data/FanCurvePreferences.kt`.
- Curve editor UI: `ui/DashboardScreen.kt`.
- Temperature-to-output response policy: `hardware/FanResponseController.kt`.

Changes to the persistence format must preserve a migration path for existing data. Add tests for loading legacy data, saving the new format, and handling invalid input. Never use a mutable display name as a stable identifier.

## Adapting a new device

Follow `docs/FAN_ADAPTATION.md` and use read-only commands first to identify thermal zones, hwmon nodes, cooling devices, their ranges, permissions, and behavior.

- Add new thermal-type naming conventions centrally in `ThermalClassifier.kt`, with classification tests.
- If a device has different fan-node names, PWM ranges, polarity, or enable semantics, introduce an explicit hardware configuration model near `FanController.kt`.
- When differences span several devices, prefer capability-based hardware profiles over device-model branches in the control loop.
- Keep fan writes restricted to fan-output nodes and performance writes restricted to cpufreq policy minimum/maximum nodes. Leave kernel overheat protection enabled.

Real-device validation should cover 0%, an intermediate value, 100%, screen off, unlock, service restart, and root-shell recreation.

## Extending thermal data and telemetry

- Sensor discovery and value validation: `hardware/ThermalTelemetry.kt`.
- Shared runtime state: `hardware/TelemetryRepository.kt`.
- Overlay presentation: `overlay/TelemetryOverlay.kt`.
- Notification and background refresh: `service/SystemControlService.kt`.

When adding a sensor, first decide whether it is display-only or contributes to control. Control calculations should be defined once in `ThermalSnapshot` and secured with unit tests so that the dashboard, overlay, and service cannot produce different results.

## Extending background control

`SystemControlService` is the sole owner of hardware writes and telemetry polling. New control strategies should retain these boundaries:

- The UI only changes configuration or sends explicit actions.
- The service owns scheduling, lifecycle, and hardware calls.
- `FanResponseController` owns independently testable filtering, deadband, and ramping algorithms.
- `FanController` owns sysfs discovery, range mapping, and writes.
- `CpuFrequencyController` owns cpufreq policy discovery, maximum-frequency writes, and post-write verification.
- `TelemetryRepository` publishes runtime state.

Changes to the loop interval, filter window, or ramp duration should be tested with irregular time intervals to cover scheduler jitter and boundary conditions.

## Suggested review checklist

- Hardware access has not been introduced into UI or feature modules.
- Kernel thermal protection, governors, and hardware nodes outside the feature being changed remain untouched.
- New persisted fields have defaults and a migration strategy.
- New user-visible text has been localized.
- New logic has tests for normal and boundary cases.
- Documentation describes the current design and extension points rather than temporary debugging history.
- `:app:testDebugUnitTest` and `:app:assembleDebug` both pass.
