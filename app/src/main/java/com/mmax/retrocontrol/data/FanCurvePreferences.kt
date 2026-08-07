package com.mmax.retrocontrol.data

import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Persistent repository for a configurable fan-curve catalog.
 *
 * Version 1 stored four enum-backed curves in separate preference keys.
 * Version 2 stores a list of independently named curves, each with current and
 * reset-default points. Loading performs a lossless one-time migration.
 */
object FanCurvePreferences {
    private const val FORMAT_VERSION = 2
    private const val OFF = "OFF"

    fun load(prefs: SharedPreferences): FanControlConfig {
        val storedCatalog = prefs.getString(Prefs.FAN_CURVE_CATALOG, null)
        val catalog = storedCatalog
            ?.let(::decodeCatalog)
            ?: migrateLegacyCatalog(prefs)
        val rawSelection = prefs.getString(Prefs.FAN_MODE, null)
        val activeId = resolveActiveId(rawSelection, catalog)
        val normalizedSelection = activeId ?: OFF

        if (
            storedCatalog == null ||
            hasLegacyArchivedField(storedCatalog) ||
            rawSelection != normalizedSelection
        ) {
            prefs.edit {
                putString(Prefs.FAN_CURVE_CATALOG, encodeCatalog(catalog))
                putString(Prefs.FAN_MODE, normalizedSelection)
                activeId?.let { putString(Prefs.LAST_FAN_CURVE, it) }
            }
        }
        return FanControlConfig(catalog = catalog, activeProfileId = activeId)
    }

    fun select(prefs: SharedPreferences, profileId: String?): FanControlConfig {
        val current = load(prefs)
        val selectedId = profileId?.takeIf { current.catalog.profile(it) != null }
        prefs.edit {
            putString(Prefs.FAN_MODE, selectedId ?: OFF)
            selectedId?.let { putString(Prefs.LAST_FAN_CURVE, it) }
        }
        return current.copy(activeProfileId = selectedId)
    }

    fun toggle(prefs: SharedPreferences): FanControlConfig {
        val current = load(prefs)
        if (current.enabled) return select(prefs, null)
        val last = prefs.getString(Prefs.LAST_FAN_CURVE, null)
        val target = current.catalog.profile(last)?.id
            ?: current.catalog.profiles.firstOrNull()?.id
        return select(prefs, target)
    }

    fun savePoints(
        prefs: SharedPreferences,
        profileId: String,
        points: List<FanCurvePoint>,
    ): FanControlConfig = updateProfile(prefs, profileId) { it.withPoints(points) }

    fun setCurrentAsDefault(
        prefs: SharedPreferences,
        profileId: String,
        points: List<FanCurvePoint>,
    ): FanControlConfig = updateProfile(prefs, profileId) {
        it.withCurrentAsDefault(points)
    }

    fun reset(
        prefs: SharedPreferences,
        profileId: String,
    ): FanControlConfig = updateProfile(prefs, profileId, FanCurveProfile::reset)

    fun rename(
        prefs: SharedPreferences,
        profileId: String,
        name: String,
    ): FanControlConfig = updateProfile(prefs, profileId) { it.renamed(name) }

    fun add(
        prefs: SharedPreferences,
        name: String,
        templatePoints: List<FanCurvePoint>,
    ): FanControlConfig {
        val current = load(prefs)
        val clean = FanCurveSerializer.sanitize(templatePoints).takeIf { it.size >= 2 }
            ?: BuiltInFanCurve.NORMAL.factoryPoints
        val profile = FanCurveProfile(
            id = "curve-${UUID.randomUUID()}",
            customName = name.trim().take(40).ifBlank { "Fan curve" },
            points = clean,
            defaultPoints = clean,
        )
        val updated = current.copy(
            catalog = current.catalog.plus(profile),
        )
        persist(prefs, updated)
        return updated
    }

    fun addImported(
        prefs: SharedPreferences,
        name: String,
        points: List<FanCurvePoint>,
        defaultPoints: List<FanCurvePoint>,
    ): FanControlConfig {
        val current = load(prefs)
        val cleanPoints = FanCurveSerializer.sanitize(points)
        val cleanDefaults = FanCurveSerializer.sanitize(defaultPoints)
        require(cleanPoints.size >= 2 && cleanDefaults.size >= 2) {
            "A fan curve requires at least two points"
        }
        val profile = FanCurveProfile(
            id = "curve-${UUID.randomUUID()}",
            customName = name.trim().take(40).ifBlank { "Fan curve" },
            points = cleanPoints,
            defaultPoints = cleanDefaults,
        )
        return current.copy(catalog = current.catalog.plus(profile)).also {
            persist(prefs, it)
        }
    }

    fun delete(
        prefs: SharedPreferences,
        profileId: String,
    ): FanControlConfig {
        val current = load(prefs)
        if (current.catalog.profile(profileId) == null) return current
        val catalog = current.catalog.remove(profileId)
        val activeId = current.activeProfileId
            ?.takeUnless { it == profileId }
            ?.takeIf { catalog.profile(it) != null }
        val updated = FanControlConfig(catalog = catalog, activeProfileId = activeId)
        persist(prefs, updated)
        return updated
    }

    fun adjustAroundTemperature(
        prefs: SharedPreferences,
        profileId: String,
        tempC: Double,
        deltaPercent: Int,
    ): FanControlConfig {
        val current = load(prefs)
        val profile = current.catalog.profile(profileId) ?: return current
        val points = profile.points
        if (points.size < 2) return current

        val upperIndex = when {
            tempC <= points.first().tempC -> 1
            tempC >= points.last().tempC -> points.lastIndex
            else -> points.indexOfFirst { tempC <= it.tempC }.coerceAtLeast(1)
        }
        val lowerIndex = upperIndex - 1
        val adjusted = points.mapIndexed { index, point ->
            if (index == lowerIndex || index == upperIndex) {
                point.copy(
                    speedPercent = (point.speedPercent + deltaPercent).coerceIn(0, 100)
                )
            } else {
                point
            }
        }
        return savePoints(prefs, profileId, adjusted)
    }

    private fun updateProfile(
        prefs: SharedPreferences,
        profileId: String,
        transform: (FanCurveProfile) -> FanCurveProfile,
    ): FanControlConfig {
        val current = load(prefs)
        val profile = current.catalog.profile(profileId) ?: return current
        val updated = current.copy(catalog = current.catalog.replace(transform(profile)))
        persist(prefs, updated)
        return updated
    }

    private fun persist(prefs: SharedPreferences, config: FanControlConfig) {
        prefs.edit {
            putString(Prefs.FAN_CURVE_CATALOG, encodeCatalog(config.catalog))
            putString(Prefs.FAN_MODE, config.activeProfileId ?: OFF)
            config.activeProfileId?.let { putString(Prefs.LAST_FAN_CURVE, it) }
        }
    }

    private fun resolveActiveId(
        raw: String?,
        catalog: FanCurveCatalog,
    ): String? {
        if (raw.equals(OFF, ignoreCase = true)) return null
        val migratedId = when (raw?.uppercase()) {
            "QUIET" -> BuiltInFanCurve.QUIET.id
            "NORMAL", null -> BuiltInFanCurve.NORMAL.id
            "PERFORMANCE", "SPORT" -> BuiltInFanCurve.PERFORMANCE.id
            "CUSTOM" -> BuiltInFanCurve.LEGACY_CUSTOM.id
            else -> raw
        }
        return catalog.profile(migratedId)?.id ?: if (raw == null) {
            catalog.profile(BuiltInFanCurve.NORMAL.id)?.id
                ?: catalog.profiles.firstOrNull()?.id
        } else {
            null
        }
    }

    private fun migrateLegacyCatalog(prefs: SharedPreferences): FanCurveCatalog {
        val profiles = FanCurveCatalog.factoryProfiles().map { profile ->
            val legacy = prefs.getString(legacyKey(profile.builtIn), null)
            profile.copy(
                points = FanCurveSerializer.parse(legacy, profile.defaultPoints),
            )
        }.toMutableList()

        val legacySelection = prefs.getString(Prefs.FAN_MODE, null)
        val legacyLast = prefs.getString(Prefs.LAST_FAN_CURVE, null)
        val legacyCustomValue = prefs.getString(Prefs.FAN_CURVE_CUSTOM, null)
            ?: prefs.getString(Prefs.LEGACY_FAN_CURVE_CUSTOM, null)
        val needsLegacyCustom = legacyCustomValue != null ||
            legacySelection.equals("CUSTOM", ignoreCase = true) ||
            legacyLast.equals("CUSTOM", ignoreCase = true)
        if (needsLegacyCustom) {
            val builtIn = BuiltInFanCurve.LEGACY_CUSTOM
            profiles += FanCurveProfile(
                id = builtIn.id,
                builtIn = builtIn,
                points = FanCurveSerializer.parse(legacyCustomValue, builtIn.factoryPoints),
                defaultPoints = builtIn.factoryPoints,
            )
        }
        return FanCurveCatalog(profiles)
    }

    private fun legacyKey(builtIn: BuiltInFanCurve?): String? = when (builtIn) {
        BuiltInFanCurve.QUIET -> Prefs.FAN_CURVE_QUIET
        BuiltInFanCurve.NORMAL -> Prefs.FAN_CURVE_NORMAL
        BuiltInFanCurve.PERFORMANCE -> Prefs.FAN_CURVE_PERFORMANCE
        BuiltInFanCurve.LEGACY_CUSTOM -> Prefs.FAN_CURVE_CUSTOM
        null -> null
    }

    private fun encodeCatalog(catalog: FanCurveCatalog): String {
        val profiles = JSONArray()
        catalog.profiles.forEach { profile ->
            profiles.put(
                JSONObject()
                    .put("id", profile.id)
                    .apply {
                        profile.builtIn?.let { put("builtIn", it.id) }
                        profile.customName?.let { put("name", it) }
                    }
                    .put("points", FanCurveSerializer.serialize(profile.points))
                    .put("defaultPoints", FanCurveSerializer.serialize(profile.defaultPoints))
            )
        }
        return JSONObject()
            .put("version", FORMAT_VERSION)
            .put("profiles", profiles)
            .toString()
    }

    private fun hasLegacyArchivedField(value: String): Boolean = runCatching {
        val entries = JSONObject(value).getJSONArray("profiles")
        (0 until entries.length()).any { index ->
            entries.getJSONObject(index).has("archived")
        }
    }.getOrDefault(false)

    private fun decodeCatalog(value: String): FanCurveCatalog? = runCatching {
        val root = JSONObject(value)
        require(root.optInt("version") == FORMAT_VERSION)
        val entries = root.getJSONArray("profiles")
        val profiles = buildList {
            repeat(entries.length()) { index ->
                val item = entries.getJSONObject(index)
                if (item.optBoolean("archived", false)) return@repeat
                val id = item.getString("id")
                val builtIn = BuiltInFanCurve.fromId(item.optString("builtIn"))
                val fallback = builtIn?.factoryPoints ?: BuiltInFanCurve.NORMAL.factoryPoints
                add(
                    FanCurveProfile(
                        id = id,
                        builtIn = builtIn,
                        customName = item.optString("name").takeIf { it.isNotBlank() },
                        points = FanCurveSerializer.parse(item.optString("points"), fallback),
                        defaultPoints = FanCurveSerializer.parse(
                            item.optString("defaultPoints"),
                            fallback,
                        ),
                    )
                )
            }
        }.distinctBy { it.id }
        FanCurveCatalog(profiles)
    }.getOrNull()
}
