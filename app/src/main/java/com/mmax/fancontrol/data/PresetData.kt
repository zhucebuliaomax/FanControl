package com.mmax.retrocontrol.data

data class ControlPreset(
    val id: String,
    val name: String,
    val isDefault: Boolean = false,
    /** Null represents Off. */
    val fanCurveId: String? = BuiltInFanCurve.NORMAL.id,
    val joystickId: String? = null,
    val buttonLayoutId: String? = null,
    val performanceProfileId: String? = null,
) {
    init {
        require(id.isNotBlank()) { "A preset requires an id" }
        require(name.isNotBlank()) { "A preset requires a name" }
    }

    fun renamed(value: String): ControlPreset =
        copy(name = value.trim().take(40).ifBlank { name })
}

data class ControlPresetCatalog(
    val presets: List<ControlPreset> = listOf(defaultPreset()),
) {
    fun preset(id: String?): ControlPreset? = presets.firstOrNull { it.id == id }

    fun replace(preset: ControlPreset): ControlPresetCatalog = copy(
        presets = presets.map { existing ->
            if (existing.id == preset.id) preset else existing
        }
    )

    fun plus(preset: ControlPreset): ControlPresetCatalog =
        copy(presets = presets.filterNot { it.id == preset.id } + preset)

    fun remove(id: String): ControlPresetCatalog =
        copy(presets = presets.filterNot { it.id == id })

    companion object {
        const val DEFAULT_ID = "default"

        fun defaultPreset(): ControlPreset = ControlPreset(
            id = DEFAULT_ID,
            name = "Default",
            isDefault = true,
            fanCurveId = BuiltInFanCurve.NORMAL.id,
        )
    }
}

data class ControlPresetConfig(
    val catalog: ControlPresetCatalog = ControlPresetCatalog(),
    val selectedPresetId: String = ControlPresetCatalog.DEFAULT_ID,
) {
    val selectedPreset: ControlPreset
        get() = catalog.preset(selectedPresetId)
            ?: ControlPresetCatalog.defaultPreset()
}

sealed interface FanSelectionSource {
    data object FollowPreset : FanSelectionSource
    data class DirectCurve(val profileId: String) : FanSelectionSource
}

data class FanSelectionConfig(
    val source: FanSelectionSource,
    val enabled: Boolean,
)
