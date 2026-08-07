package com.mmax.retrocontrol

import com.mmax.retrocontrol.data.AppControlProfile
import com.mmax.retrocontrol.data.BuiltInPerformanceProfile
import com.mmax.retrocontrol.data.ControlPreset
import com.mmax.retrocontrol.data.ControlPresetCatalog
import com.mmax.retrocontrol.data.ControlPresetConfig
import com.mmax.retrocontrol.data.CpuFrequencyPolicy
import com.mmax.retrocontrol.data.PerformanceProfile
import com.mmax.retrocontrol.data.PerformanceProfileConfig
import com.mmax.retrocontrol.data.PerformanceProfilePreferences
import com.mmax.retrocontrol.data.PerformanceProfileResolver
import com.mmax.retrocontrol.hardware.CpuFrequencyController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceProfileTest {
    private val policies = listOf(
        CpuFrequencyPolicy(
            id = 0,
            cpuIds = listOf(0, 1, 2),
            supportedFrequencies = listOf(307_200, 1_344_000, 1_785_600, 2_016_000),
            currentMinFrequency = 556_800,
            currentMaxFrequency = 2_016_000,
            stockMinFrequency = 307_200,
            stockMaxFrequency = 2_016_000,
        ),
        CpuFrequencyPolicy(
            id = 3,
            cpuIds = listOf(3, 4, 5, 6),
            supportedFrequencies = listOf(499_200, 1_920_000, 2_323_200, 2_707_200),
            currentMinFrequency = 499_200,
            currentMaxFrequency = 2_707_200,
            stockMinFrequency = 499_200,
            stockMaxFrequency = 2_803_200,
        ),
    )

    @Test
    fun policyParser_keepsCpuZeroAndStockLimits() {
        val parsed = CpuFrequencyController.parsePolicyLines(
            listOf(
                "0|0 1 2|307200 1344000 2016000|556800|672000|307200|2016000",
                "3|3 4 5 6|499200 1920000 2707200|499200|1286400|499200|2803200",
            )
        )

        assertEquals(listOf(0, 3), parsed.map { it.id })
        assertEquals(listOf(0, 1, 2), parsed.first().cpuIds)
        assertEquals(2_803_200, parsed.last().stockMaxFrequency)
    }

    @Test
    fun factoryProfiles_snapRatiosToSupportedSteps() {
        val profiles = PerformanceProfilePreferences.factoryProfiles(policies)
        val balanced = profiles.first { it.builtIn == BuiltInPerformanceProfile.BALANCED }
        val efficient = profiles.first { it.builtIn == BuiltInPerformanceProfile.EFFICIENT }

        assertEquals(1_785_600, balanced.maxFrequencies[0])
        assertEquals(2_323_200, balanced.maxFrequencies[3])
        assertEquals(1_344_000, efficient.maxFrequencies[0])
        assertEquals(1_920_000, efficient.maxFrequencies[3])
    }

    @Test
    fun applyScript_capsPoliciesAndLocksOnlyMaximumNodes() {
        val profile = PerformanceProfile(
            id = "custom",
            customName = "Custom",
            maxFrequencies = mapOf(0 to 1_344_000, 3 to 1_920_000),
        )

        val script = CpuFrequencyController.buildApplyScript(profile, policies)

        assertTrue(script.contains("echo 1344000 > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq"))
        assertTrue(script.contains("chmod 0444 /sys/devices/system/cpu/cpufreq/policy3/scaling_max_freq"))
        assertTrue(script.contains("scaling_min_freq"))
        assertFalse(script.contains("PServer"))
        assertFalse(script.contains("stop "))
    }

    @Test
    fun stockScriptRestoresWritableMaximumNodes() {
        val stock = PerformanceProfilePreferences.factoryProfiles(policies)
            .first { it.builtIn == BuiltInPerformanceProfile.STOCK }

        val script = CpuFrequencyController.buildApplyScript(stock, policies)

        assertTrue(script.contains("echo 2803200 > /sys/devices/system/cpu/cpufreq/policy3/scaling_max_freq"))
        assertTrue(script.contains("chmod 0644 /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq"))
        assertFalse(script.contains("chmod 0444"))
    }

    @Test
    fun appPerformanceProfileOverridesPresetSelection() {
        val stock = PerformanceProfile(
            id = BuiltInPerformanceProfile.STOCK.id,
            builtIn = BuiltInPerformanceProfile.STOCK,
            maxFrequencies = mapOf(0 to 2_016_000, 3 to 2_803_200),
        )
        val efficient = PerformanceProfile(
            id = "efficient",
            customName = "Efficient",
            maxFrequencies = mapOf(0 to 1_344_000, 3 to 1_920_000),
        )
        val preset = ControlPreset(
            id = ControlPresetCatalog.DEFAULT_ID,
            name = "Default",
            isDefault = true,
            performanceProfileId = efficient.id,
        )
        val presetConfig = ControlPresetConfig(
            catalog = ControlPresetCatalog(listOf(preset)),
        )
        val profileConfig = PerformanceProfileConfig(
            policies = policies,
            profiles = listOf(stock, efficient),
        )

        assertEquals(
            stock.id,
            PerformanceProfileResolver.resolveTargetProfileId(
                profileConfig = profileConfig,
                presetConfig = presetConfig,
                appProfile = AppControlProfile(
                    packageName = "example.game",
                    performanceProfileId = stock.id,
                ),
                appIsGame = true,
            ),
        )
        assertEquals(
            efficient.id,
            PerformanceProfileResolver.resolveTargetProfileId(
                profileConfig = profileConfig,
                presetConfig = presetConfig,
                appIsGame = true,
            ),
        )
    }
}
