package com.mmax.retrocontrol.ui

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.graphics.drawable.toBitmap
import com.mmax.retrocontrol.data.AppControlProfile
import com.mmax.retrocontrol.data.AppProfilePreferences
import com.mmax.retrocontrol.data.BuiltInFanCurve
import com.mmax.retrocontrol.data.ControlPresetConfig
import com.mmax.retrocontrol.data.FanControlConfig
import com.mmax.retrocontrol.data.FanCurvePoint
import com.mmax.retrocontrol.data.FanCurvePreferences
import com.mmax.retrocontrol.data.FanSelectionConfig
import com.mmax.retrocontrol.data.FanSelectionPreferences
import com.mmax.retrocontrol.data.PresetPreferences
import com.mmax.retrocontrol.data.Prefs
import com.mmax.retrocontrol.hardware.TelemetryRepository
import com.mmax.retrocontrol.hardware.TelemetrySnapshot
import com.mmax.retrocontrol.service.SystemControlService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val icon: Bitmap?,
    val isGame: Boolean,
    val profileSummary: String = "",
)

data class DashboardState(
    val fanConfig: FanControlConfig = FanControlConfig(),
    val presetConfig: ControlPresetConfig = ControlPresetConfig(),
    val fanSelection: FanSelectionConfig = FanSelectionConfig(
        source = com.mmax.retrocontrol.data.FanSelectionSource.FollowPreset,
        enabled = true,
    ),
    val overlayEnabled: Boolean = false,
    val autoStartEnabled: Boolean = false,
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val appProfiles: Map<String, AppControlProfile> = emptyMap(),
    val telemetry: TelemetrySnapshot = TelemetrySnapshot(),
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(Prefs.FILE, Application.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = mutableState.asStateFlow()

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        loadPreferences()
    }

    init {
        loadPreferences()
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
        viewModelScope.launch {
            TelemetryRepository.state.collectLatest { telemetry ->
                mutableState.update { it.copy(telemetry = telemetry) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val launcherApps = application.getSystemService(LauncherApps::class.java)
            val apps = runCatching {
                launcherApps.getActivityList(null, Process.myUserHandle())
                    .distinctBy { it.applicationInfo.packageName }
                    .map { activity ->
                        InstalledAppInfo(
                            label = activity.label.toString(),
                            packageName = activity.applicationInfo.packageName,
                            icon = runCatching {
                                activity.getBadgedIcon(0).toBitmap(width = 96, height = 96)
                            }.getOrNull(),
                            isGame = activity.applicationInfo.category ==
                                ApplicationInfo.CATEGORY_GAME,
                        )
                    }
                    .sortedWith(
                        compareBy(String.CASE_INSENSITIVE_ORDER, InstalledAppInfo::label)
                            .thenBy(InstalledAppInfo::packageName)
                    )
            }.getOrDefault(emptyList())
            mutableState.update { it.copy(installedApps = apps) }
        }
    }

    override fun onCleared() {
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        super.onCleared()
    }

    private fun loadPreferences() {
        val rawFanConfig = FanCurvePreferences.load(prefs)
        val fanConfig = FanSelectionPreferences.apply(prefs, rawFanConfig)
        val curveIds = fanConfig.catalog.profiles.mapTo(mutableSetOf()) { it.id }
        val presetConfig = PresetPreferences.load(prefs, curveIds)
        val presetIds = presetConfig.catalog.presets.mapTo(mutableSetOf()) { it.id }
        mutableState.update {
            it.copy(
                fanConfig = fanConfig,
                presetConfig = presetConfig,
                fanSelection = FanSelectionPreferences.load(prefs, fanConfig),
                appProfiles = AppProfilePreferences.load(prefs, presetIds, curveIds),
                overlayEnabled = prefs.getBoolean(Prefs.OVERLAY_ENABLED, false),
                autoStartEnabled = prefs.getBoolean(Prefs.AUTO_START_ENABLED, false),
            )
        }
    }

    fun selectFanProfile(profileId: String?) {
        val config = if (profileId == null) {
            FanCurvePreferences.select(prefs, null)
        } else {
            FanSelectionPreferences.selectDirectCurve(prefs, profileId)
        }
        mutableState.update { it.copy(fanConfig = config) }
        SystemControlService.startOrUpdate(getApplication())
    }

    fun addFanCurve(name: String): String {
        val existingIds = mutableState.value.fanConfig.catalog.profiles.mapTo(mutableSetOf()) { it.id }
        val template = mutableState.value.fanConfig.activeProfile?.points
            ?: BuiltInFanCurve.NORMAL.factoryPoints
        val config = FanCurvePreferences.add(prefs, name, template)
        mutableState.update { it.copy(fanConfig = config) }
        SystemControlService.startOrUpdate(getApplication())
        return requireNotNull(config.catalog.profiles.firstOrNull { it.id !in existingIds }?.id)
    }

    fun setFanCurve(profileId: String, points: List<FanCurvePoint>) {
        val config = runCatching {
            FanCurvePreferences.savePoints(prefs, profileId, points)
        }.getOrNull() ?: return
        mutableState.update { it.copy(fanConfig = config) }
        SystemControlService.startOrUpdate(getApplication())
    }

    fun setFanCurveAsDefault(profileId: String, points: List<FanCurvePoint>) {
        val config = runCatching {
            FanCurvePreferences.setCurrentAsDefault(prefs, profileId, points)
        }.getOrNull() ?: return
        mutableState.update { it.copy(fanConfig = config) }
        SystemControlService.startOrUpdate(getApplication())
    }

    fun resetFanCurve(profileId: String) {
        val config = FanCurvePreferences.reset(prefs, profileId)
        mutableState.update { it.copy(fanConfig = config) }
        SystemControlService.startOrUpdate(getApplication())
    }

    fun renameFanCurve(profileId: String, name: String) {
        val config = FanCurvePreferences.rename(prefs, profileId, name)
        mutableState.update { it.copy(fanConfig = config) }
        SystemControlService.startOrUpdate(getApplication())
    }

    fun deleteFanCurve(profileId: String) {
        val catalogConfig = FanCurvePreferences.delete(prefs, profileId)
        val curveIds = catalogConfig.catalog.profiles.mapTo(mutableSetOf()) { it.id }
        PresetPreferences.load(prefs, curveIds)
        val config = FanSelectionPreferences.apply(prefs, catalogConfig)
        loadPreferences()
        SystemControlService.startOrUpdate(getApplication())
    }

    fun addPreset(name: String): String {
        val curveIds = mutableState.value.fanConfig.catalog.profiles.mapTo(mutableSetOf()) { it.id }
        val (_, id) = PresetPreferences.add(prefs, name, curveIds)
        loadPreferences()
        return id
    }

    fun renamePreset(presetId: String, name: String) {
        val curveIds = mutableState.value.fanConfig.catalog.profiles.mapTo(mutableSetOf()) { it.id }
        PresetPreferences.rename(prefs, presetId, name, curveIds)
        loadPreferences()
    }

    fun deletePreset(presetId: String) {
        val curveIds = mutableState.value.fanConfig.catalog.profiles.mapTo(mutableSetOf()) { it.id }
        PresetPreferences.delete(prefs, presetId, curveIds)
        FanSelectionPreferences.apply(prefs)
        loadPreferences()
        SystemControlService.startOrUpdate(getApplication())
    }

    fun setPresetFanCurve(presetId: String, profileId: String?) {
        val curveIds = mutableState.value.fanConfig.catalog.profiles.mapTo(mutableSetOf()) { it.id }
        PresetPreferences.setFanCurve(prefs, presetId, profileId, curveIds)
        FanSelectionPreferences.apply(prefs)
        loadPreferences()
        SystemControlService.startOrUpdate(getApplication())
    }

    fun selectGlobalPreset(presetId: String) {
        val curveIds = mutableState.value.fanConfig.catalog.profiles.mapTo(mutableSetOf()) { it.id }
        PresetPreferences.select(prefs, presetId, curveIds)
        FanSelectionPreferences.apply(prefs)
        loadPreferences()
        SystemControlService.startOrUpdate(getApplication())
    }

    fun setAppPreset(packageName: String, presetId: String) {
        val state = mutableState.value
        AppProfilePreferences.setPreset(
            prefs = prefs,
            packageName = packageName,
            presetId = presetId,
            availablePresetIds = state.presetConfig.catalog.presets
                .mapTo(mutableSetOf()) { it.id },
            availableFanCurveIds = state.fanConfig.catalog.profiles
                .mapTo(mutableSetOf()) { it.id },
        )
        loadPreferences()
        SystemControlService.startOrUpdate(getApplication())
    }

    fun setAppFanCurve(packageName: String, profileId: String?) {
        val state = mutableState.value
        AppProfilePreferences.setFanCurve(
            prefs = prefs,
            packageName = packageName,
            fanCurveId = profileId,
            availablePresetIds = state.presetConfig.catalog.presets
                .mapTo(mutableSetOf()) { it.id },
            availableFanCurveIds = state.fanConfig.catalog.profiles
                .mapTo(mutableSetOf()) { it.id },
        )
        loadPreferences()
        SystemControlService.startOrUpdate(getApplication())
    }

    fun setOverlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Prefs.OVERLAY_ENABLED, enabled).apply()
        mutableState.update { it.copy(overlayEnabled = enabled) }
        SystemControlService.startOrUpdate(getApplication())
    }

    fun setAutoStartEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Prefs.AUTO_START_ENABLED, enabled).apply()
        mutableState.update { it.copy(autoStartEnabled = enabled) }
    }
}
