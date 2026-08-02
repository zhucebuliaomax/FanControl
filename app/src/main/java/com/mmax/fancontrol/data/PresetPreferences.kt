package com.mmax.retrocontrol.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object PresetPreferences {
    fun load(
        prefs: SharedPreferences,
        availableFanCurveIds: Set<String>,
    ): ControlPresetConfig {
        val stored = prefs.getString(Prefs.PRESET_CATALOG, null)
        val decoded = stored?.let(::decodeCatalog) ?: ControlPresetCatalog()
        val normalizedPresets = decoded.presets
            .ifEmpty { ControlPresetCatalog().presets }
            .map { preset ->
                preset.copy(
                    fanCurveId = preset.fanCurveId?.takeIf(availableFanCurveIds::contains),
                )
            }
            .let { presets ->
                if (presets.any(ControlPreset::isDefault)) presets
                else listOf(ControlPresetCatalog.defaultPreset()) + presets
            }
        val catalog = ControlPresetCatalog(normalizedPresets)
        val selectedId = prefs.getString(Prefs.SELECTED_PRESET, null)
            ?.takeIf { catalog.preset(it) != null }
            ?: catalog.presets.first { it.isDefault }.id
        val config = ControlPresetConfig(catalog, selectedId)

        if (stored == null || decoded != catalog || prefs.getString(Prefs.SELECTED_PRESET, null) != selectedId) {
            persist(prefs, config)
        }
        return config
    }

    fun select(
        prefs: SharedPreferences,
        presetId: String,
        availableFanCurveIds: Set<String>,
    ): ControlPresetConfig {
        val current = load(prefs, availableFanCurveIds)
        val selectedId = presetId.takeIf { current.catalog.preset(it) != null }
            ?: current.selectedPresetId
        return current.copy(selectedPresetId = selectedId).also { persist(prefs, it) }
    }

    fun add(
        prefs: SharedPreferences,
        name: String,
        availableFanCurveIds: Set<String>,
    ): Pair<ControlPresetConfig, String> {
        val current = load(prefs, availableFanCurveIds)
        val template = current.selectedPreset
        val id = "preset-${UUID.randomUUID()}"
        val preset = template.copy(
            id = id,
            name = name.trim().take(40).ifBlank { "New preset" },
            isDefault = false,
        )
        val updated = current.copy(catalog = current.catalog.plus(preset))
        persist(prefs, updated)
        return updated to id
    }

    fun rename(
        prefs: SharedPreferences,
        presetId: String,
        name: String,
        availableFanCurveIds: Set<String>,
    ): ControlPresetConfig = update(prefs, presetId, availableFanCurveIds) {
        it.renamed(name)
    }

    fun setFanCurve(
        prefs: SharedPreferences,
        presetId: String,
        fanCurveId: String?,
        availableFanCurveIds: Set<String>,
    ): ControlPresetConfig = update(prefs, presetId, availableFanCurveIds) {
        it.copy(fanCurveId = fanCurveId?.takeIf(availableFanCurveIds::contains))
    }

    fun delete(
        prefs: SharedPreferences,
        presetId: String,
        availableFanCurveIds: Set<String>,
    ): ControlPresetConfig {
        val current = load(prefs, availableFanCurveIds)
        val target = current.catalog.preset(presetId) ?: return current
        if (target.isDefault) return current
        val catalog = current.catalog.remove(presetId)
        val selectedId = current.selectedPresetId.takeIf { catalog.preset(it) != null }
            ?: catalog.presets.first { it.isDefault }.id
        return ControlPresetConfig(catalog, selectedId).also { persist(prefs, it) }
    }

    private fun update(
        prefs: SharedPreferences,
        presetId: String,
        availableFanCurveIds: Set<String>,
        transform: (ControlPreset) -> ControlPreset,
    ): ControlPresetConfig {
        val current = load(prefs, availableFanCurveIds)
        val target = current.catalog.preset(presetId) ?: return current
        val updated = current.copy(catalog = current.catalog.replace(transform(target)))
        persist(prefs, updated)
        return updated
    }

    private fun persist(prefs: SharedPreferences, config: ControlPresetConfig) {
        prefs.edit()
            .putString(Prefs.PRESET_CATALOG, encodeCatalog(config.catalog))
            .putString(Prefs.SELECTED_PRESET, config.selectedPresetId)
            .apply()
    }

    private fun encodeCatalog(catalog: ControlPresetCatalog): String {
        val presets = JSONArray()
        catalog.presets.forEach { preset ->
            presets.put(
                JSONObject()
                    .put("id", preset.id)
                    .put("name", preset.name)
                    .put("default", preset.isDefault)
                    .put("fanCurveId", preset.fanCurveId ?: JSONObject.NULL)
                    .put("joystickId", preset.joystickId ?: JSONObject.NULL)
                    .put("buttonLayoutId", preset.buttonLayoutId ?: JSONObject.NULL)
                    .put("performanceProfileId", preset.performanceProfileId ?: JSONObject.NULL)
            )
        }
        return JSONObject().put("presets", presets).toString()
    }

    private fun decodeCatalog(serialized: String): ControlPresetCatalog = runCatching {
        val array = JSONObject(serialized).getJSONArray("presets")
        val presets = buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val id = item.getString("id").takeIf(String::isNotBlank) ?: continue
                val name = item.optString("name").takeIf(String::isNotBlank) ?: continue
                add(
                    ControlPreset(
                        id = id,
                        name = name,
                        isDefault = item.optBoolean("default", false),
                        fanCurveId = item.nullableString("fanCurveId"),
                        joystickId = item.nullableString("joystickId"),
                        buttonLayoutId = item.nullableString("buttonLayoutId"),
                        performanceProfileId = item.nullableString("performanceProfileId"),
                    )
                )
            }
        }
        ControlPresetCatalog(presets)
    }.getOrElse { ControlPresetCatalog() }

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
}

object FanSelectionPreferences {
    private const val SOURCE_PRESET = "PRESET"
    private const val SOURCE_CURVE = "CURVE"

    fun load(prefs: SharedPreferences, fanConfig: FanControlConfig): FanSelectionConfig {
        val storedSource = prefs.getString(Prefs.FAN_SELECTION_SOURCE, null)
        val source = when (storedSource) {
            SOURCE_PRESET -> FanSelectionSource.FollowPreset
            SOURCE_CURVE -> prefs.getString(Prefs.FAN_SELECTION_CURVE, null)
                ?.takeIf { fanConfig.catalog.profile(it) != null }
                ?.let(FanSelectionSource::DirectCurve)
                ?: FanSelectionSource.FollowPreset
            else -> fanConfig.activeProfileId
                ?.let(FanSelectionSource::DirectCurve)
                ?: FanSelectionSource.FollowPreset
        }
        val enabled = if (prefs.contains(Prefs.FAN_TILE_ENABLED)) {
            prefs.getBoolean(Prefs.FAN_TILE_ENABLED, true)
        } else {
            fanConfig.enabled
        }
        val config = FanSelectionConfig(source, enabled)
        if (storedSource == null || (storedSource == SOURCE_CURVE && source is FanSelectionSource.FollowPreset)) {
            persist(prefs, config)
        }
        return config
    }

    fun selectFollowPreset(prefs: SharedPreferences): FanControlConfig {
        val current = FanCurvePreferences.load(prefs)
        persist(prefs, FanSelectionConfig(FanSelectionSource.FollowPreset, enabled = true))
        return apply(prefs, current)
    }

    fun selectDirectCurve(prefs: SharedPreferences, profileId: String): FanControlConfig {
        val current = FanCurvePreferences.load(prefs)
        if (current.catalog.profile(profileId) == null) return current
        persist(
            prefs,
            FanSelectionConfig(FanSelectionSource.DirectCurve(profileId), enabled = true),
        )
        return apply(prefs, current)
    }

    fun toggle(prefs: SharedPreferences): FanControlConfig {
        val current = FanCurvePreferences.load(prefs)
        val selection = load(prefs, current)
        persist(prefs, selection.copy(enabled = !selection.enabled))
        return apply(prefs, current)
    }

    fun apply(
        prefs: SharedPreferences,
        suppliedFanConfig: FanControlConfig? = null,
    ): FanControlConfig {
        val current = suppliedFanConfig ?: FanCurvePreferences.load(prefs)
        var selection = load(prefs, current)
        if (
            selection.source is FanSelectionSource.DirectCurve &&
            current.catalog.profile(selection.source.profileId) == null
        ) {
            selection = selection.copy(source = FanSelectionSource.FollowPreset)
            persist(prefs, selection)
        }
        val presetConfig = PresetPreferences.load(
            prefs,
            current.catalog.profiles.mapTo(mutableSetOf()) { it.id },
        )
        val targetId = resolveTargetProfileId(selection, presetConfig, current.catalog)
        return if (current.activeProfileId == targetId) current
        else FanCurvePreferences.select(prefs, targetId)
    }

    internal fun resolveTargetProfileId(
        selection: FanSelectionConfig,
        presetConfig: ControlPresetConfig,
        fanCatalog: FanCurveCatalog,
    ): String? {
        if (!selection.enabled) return null
        return when (val source = selection.source) {
            FanSelectionSource.FollowPreset -> presetConfig.selectedPreset.fanCurveId
                ?.takeIf { fanCatalog.profile(it) != null }
            is FanSelectionSource.DirectCurve -> source.profileId
                .takeIf { fanCatalog.profile(it) != null }
        }
    }

    private fun persist(prefs: SharedPreferences, config: FanSelectionConfig) {
        prefs.edit()
            .putString(
                Prefs.FAN_SELECTION_SOURCE,
                if (config.source is FanSelectionSource.FollowPreset) SOURCE_PRESET else SOURCE_CURVE,
            )
            .putString(
                Prefs.FAN_SELECTION_CURVE,
                (config.source as? FanSelectionSource.DirectCurve)?.profileId,
            )
            .putBoolean(Prefs.FAN_TILE_ENABLED, config.enabled)
            .apply()
    }
}
