package com.mmax.retrocontrol.ui

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch

data class DashboardState(
    val fanConfig: FanControlConfig = FanControlConfig(),
    val presetConfig: ControlPresetConfig = ControlPresetConfig(),
    val fanSelection: FanSelectionConfig = FanSelectionConfig(
        source = com.mmax.retrocontrol.data.FanSelectionSource.FollowPreset,
        enabled = true,
    ),
    val overlayEnabled: Boolean = false,
    val autoStartEnabled: Boolean = false,
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
    }

    override fun onCleared() {
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        super.onCleared()
    }

    private fun loadPreferences() {
        val rawFanConfig = FanCurvePreferences.load(prefs)
        val fanConfig = FanSelectionPreferences.apply(prefs, rawFanConfig)
        val curveIds = fanConfig.catalog.profiles.mapTo(mutableSetOf()) { it.id }
        mutableState.update {
            it.copy(
                fanConfig = fanConfig,
                presetConfig = PresetPreferences.load(prefs, curveIds),
                fanSelection = FanSelectionPreferences.load(prefs, fanConfig),
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
