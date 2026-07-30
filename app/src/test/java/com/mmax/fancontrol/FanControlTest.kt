package com.mmax.fancontrol

import com.mmax.fancontrol.data.BuiltInFanCurve
import com.mmax.fancontrol.data.FanCurveCatalog
import com.mmax.fancontrol.data.FanCurvePoint
import com.mmax.fancontrol.data.FanCurveProfile
import com.mmax.fancontrol.data.FanCurveSerializer
import com.mmax.fancontrol.hardware.FanResponseController
import com.mmax.fancontrol.hardware.ThermalReading
import com.mmax.fancontrol.hardware.ThermalSnapshot
import com.mmax.fancontrol.hardware.ThermalKind
import com.mmax.fancontrol.hardware.DeviceThermalProfile
import com.mmax.fancontrol.hardware.ThermalProfiles
import com.mmax.fancontrol.hardware.ThermalSensorReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FanControlTest {
    @Test
    fun curveInterpolation_isLinearAndBounded() {
        val points = listOf(FanCurvePoint(40, 25), FanCurvePoint(60, 75))
        assertEquals(0.0, FanCurveSerializer.interpolate(20.0, points), 0.001)
        assertEquals(50.0, FanCurveSerializer.interpolate(50.0, points), 0.001)
        assertEquals(75.0, FanCurveSerializer.interpolate(80.0, points), 0.001)
    }

    @Test
    fun serializer_removesDuplicateTemperatures() {
        val parsed = FanCurveSerializer.parse("40:2,40:7,60:6")
        assertEquals(listOf(40, 60), parsed.map { it.tempC })
        assertEquals(2, parsed.size)
    }

    @Test
    fun catalogCurves_areIndependentAndEditable() {
        val customQuiet = listOf(FanCurvePoint(35, 15), FanCurvePoint(80, 90))
        val catalog = FanCurveCatalog()
        val quiet = requireNotNull(catalog.profile(BuiltInFanCurve.QUIET.id))
        val updated = catalog.replace(quiet.withPoints(customQuiet))

        assertEquals(customQuiet, updated.profile(BuiltInFanCurve.QUIET.id)?.points)
        assertEquals(
            BuiltInFanCurve.NORMAL.factoryPoints,
            updated.profile(BuiltInFanCurve.NORMAL.id)?.points,
        )
        assertEquals(
            BuiltInFanCurve.PERFORMANCE.factoryPoints,
            updated.profile(BuiltInFanCurve.PERFORMANCE.id)?.points,
        )
    }

    @Test
    fun resetBaseline_isOwnedByEachCurve() {
        val original = listOf(FanCurvePoint(40, 20), FanCurvePoint(80, 80))
        val changed = listOf(FanCurvePoint(40, 30), FanCurvePoint(80, 90))
        val profile = FanCurveProfile(
            id = "test",
            customName = "Test",
            points = original,
            defaultPoints = original,
        )

        assertEquals(changed, profile.withCurrentAsDefault(changed).reset().points)
    }

    @Test
    fun transientSpike_isRejectedByThreeSecondMedian() {
        val controller = FanResponseController()
        controller.resetImmediate(tempC = 50.0, percent = 25.0, nowMs = 0L)
        val samples = listOf(50.0, 50.0, 75.0, 50.0, 50.0, 50.0)
        // Curve returns 25% for temps below 60, 90% above. Median stays 50 → no trigger.
        samples.forEachIndexed { index, temp ->
            controller.update(temp, (index + 1) * 500L) { t -> if (t < 60.0) 25.0 else 90.0 }
        }
        assertEquals(25.0, controller.currentLevel(6_000L), 0.001)
    }

    @Test
    fun sustainedChange_rampsOverFiveSeconds() {
        val controller = FanResponseController()
        controller.resetImmediate(tempC = 50.0, percent = 25.0, nowMs = 0L)
        repeat(6) { index ->
            controller.update(60.0, (index + 1) * 500L) { 75.0 }
        }
        assertEquals(25.0, controller.currentLevel(3_000L), 0.001)
        assertEquals(50.0, controller.currentLevel(5_500L), 0.001)
        assertEquals(75.0, controller.currentLevel(8_000L), 0.001)
    }

    @Test
    fun outputDeadband_ignoresSmallCurveOutputChange() {
        val controller = FanResponseController()
        controller.resetImmediate(tempC = 50.0, percent = 25.0, nowMs = 0L)
        // Curve output at 51.9°C is 26.9%, diff from target 25% is 1.9% < 3% deadband.
        repeat(6) { index ->
            controller.update(51.9, (index + 1) * 500L) { t -> t * 0.5 }
        }
        assertEquals(25.0, controller.currentLevel(10_000L), 0.001)
    }

    @Test
    fun curveSwitch_canResetImmediately() {
        val controller = FanResponseController()
        controller.resetImmediate(50.0, 25.0, 0L)
        assertEquals(90.0, controller.resetImmediate(50.0, 90.0, 100L), 0.001)
        assertEquals(90.0, controller.currentLevel(100L), 0.001)
    }

    @Test
    fun thermalClassification_keepsOnlyRequestedKinds() {
        assertEquals(ThermalKind.CPU, ThermalSensorReader.classify("cpu-1-3"))
        assertEquals(ThermalKind.CPU, ThermalSensorReader.classify("cpuss-0"))
        assertEquals(ThermalKind.GPU, ThermalSensorReader.classify("gpuss-7"))
        assertEquals(ThermalKind.DDR, ThermalSensorReader.classify("ddr"))
        assertEquals(ThermalKind.BATTERY, ThermalSensorReader.classify("battery"))
        assertNull(ThermalSensorReader.classify("usb-therm"))
        assertNull(ThermalSensorReader.classify("vbat"))
    }

    @Test
    fun deviceProfile_classifiesByExactZoneName() {
        val profile = DeviceThermalProfile(
            name = "Test Device",
            cpuZones = listOf("cpu-0-0", "cpu-0-1"),
            gpuZones = listOf("gpuss-0"),
            ddrZones = listOf("ddr"),
            batteryZones = listOf("battery"),
        )

        assertEquals(ThermalKind.CPU, profile.kindOf("cpu-0-0"))
        assertEquals(ThermalKind.GPU, profile.kindOf("gpuss-0"))
        assertEquals(ThermalKind.DDR, profile.kindOf("ddr"))
        assertEquals(ThermalKind.BATTERY, profile.kindOf("battery"))
        assertNull(profile.kindOf("usb-therm"))
    }

    @Test
    fun thermalProfiles_returnsMatchingDeviceProfile() {
        val profile = ThermalProfiles.profileFor("rp6")
        assertEquals("Retroid Pocket 6 (rp6)", profile?.name)
    }

    @Test
    fun thermalProfiles_isCaseInsensitive() {
        val profile = ThermalProfiles.profileFor("RP6")
        assertEquals("Retroid Pocket 6 (rp6)", profile?.name)
    }

    @Test
    fun temperatureNormalization_supportsMilliCelsius() {
        assertEquals(42.5, ThermalSensorReader.normalize(42_500.0), 0.001)
        assertEquals(42.5, ThermalSensorReader.normalize(42.5), 0.001)
        assertTrue(ThermalSensorReader.normalize(-40_960.0) < 0.0)
    }

    @Test
    fun temperatureSummary_reportsHottestSensorName() {
        val thermal = ThermalSnapshot(
            listOf(
                ThermalReading("thermal_zone1", "cpuss-0", ThermalKind.CPU, 42.0),
                ThermalReading("thermal_zone6", "cpu-1-1", ThermalKind.CPU, 47.5),
            )
        )

        assertEquals(44.75, thermal.cpuSummary.averageC, 0.001)
        assertEquals(47.5, thermal.cpuSummary.maxC, 0.001)
        assertEquals("cpu-1-1", thermal.cpuSummary.hottest?.name)
    }
}
