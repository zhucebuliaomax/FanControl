# Automatic Thermal Zone Discovery

`ThermalSensorReader` discovers sensors at runtime by scanning
`/sys/class/thermal/thermal_zone*`. It does not use thermal-zone indices or the
Android device codename.

For every zone, the reader loads:

- `type`: the vendor-provided semantic sensor name.
- `temp`: the current temperature.

`ThermalClassifier` recognizes CPU, GPU, DDR/DRAM and battery names
case-insensitively. Examples include `cpu-1-0`, `cpuss-0`, `mtktscpu`,
`gpuss-0`, `gpu-thermal`, `ddr`, `dram-thermal` and `battery-thermal`.
Unrelated zones such as USB, modem and PMIC sensors are ignored.

This is intentionally semantic classification rather than selecting the
highest temperature from every thermal zone. An unrelated charging or PMIC
sensor can be hotter than the SoC, and some sysfs values are not temperatures.

The fan-curve control temperature is the hotter of:

1. The average of all discovered CPU sensors.
2. The average of all discovered GPU sensors.

If a vendor uses a CPU, GPU, memory or battery name without any recognizable
component word, add that naming convention to `ThermalClassifier`. A full
per-device zone list is not required.

To inspect the names exported by a device:

```shell
adb shell 'for z in /sys/class/thermal/thermal_zone*; do echo "$z/type -> $(cat "$z/type")"; done'
```
