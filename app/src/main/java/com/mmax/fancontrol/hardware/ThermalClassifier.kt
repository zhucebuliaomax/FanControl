package com.mmax.fancontrol.hardware

/**
 * How a single thermal zone type string should be matched against a rule.
 */
enum class ThermalMatchMode {
    /** Full equality, e.g. "ddr" matches only "ddr". */
    EXACT,

    /** Type starts with the pattern, e.g. "cpu-" matches "cpu-1-3". */
    PREFIX,

    /** Type contains the pattern anywhere. */
    CONTAINS,

    /** Pattern is a regular expression (case-insensitive). */
    REGEX,
}

/**
 * Maps a thermal zone type string to a [ThermalKind].
 *
 * Rules are evaluated in order; the first matching rule wins.
 */
data class ThermalClassificationRule(
    val kind: ThermalKind,
    val pattern: String,
    val mode: ThermalMatchMode = ThermalMatchMode.PREFIX,
) {
    fun matches(type: String): Boolean = when (mode) {
        ThermalMatchMode.EXACT -> type == pattern
        ThermalMatchMode.PREFIX -> type.startsWith(pattern)
        ThermalMatchMode.CONTAINS -> type.contains(pattern)
        ThermalMatchMode.REGEX -> type.matches(Regex(pattern, RegexOption.IGNORE_CASE))
    }
}

/**
 * Per-device classification profile.
 *
 * @param name Human-readable name of the device or device family.
 * @param additionalRules Rules that are evaluated **before** the generic rules.
 *   Use this to add device-specific sensors or override a generic mapping.
 * @param overrideRules If set, replaces the generic rules entirely for this device.
 */
data class DeviceThermalProfile(
    val name: String,
    val additionalRules: List<ThermalClassificationRule> = emptyList(),
    val overrideRules: List<ThermalClassificationRule>? = null,
)

/**
 * Decides the [ThermalKind] of a thermal zone based on a list of rules.
 */
class ThermalClassifier(
    private val rules: List<ThermalClassificationRule>,
) {
    fun classify(type: String): ThermalKind? =
        rules.firstOrNull { it.matches(type) }?.kind
}

/**
 * Registry of classification rules per device.
 *
 * The map keys are regular expressions (case-insensitive) matched against
 * [android.os.Build.DEVICE]. A matching profile's [DeviceThermalProfile.additionalRules]
 * are prepended to the generic rules, so device-specific rules take priority.
 */
object ThermalClassificationProfiles {

    /** Default rules used when no device profile matches or as the base for matched profiles. */
    val genericRules = listOf(
        ThermalClassificationRule(ThermalKind.CPU, "cpu-"),
        ThermalClassificationRule(ThermalKind.CPU, "cpuss-"),
        ThermalClassificationRule(ThermalKind.GPU, "gpuss-"),
        ThermalClassificationRule(ThermalKind.DDR, "ddr", ThermalMatchMode.EXACT),
        ThermalClassificationRule(ThermalKind.BATTERY, "battery", ThermalMatchMode.EXACT),
    )

    /**
     * Device-specific profiles.
     *
     * To add a new device, add an entry here keyed by a regex that matches
     * [android.os.Build.DEVICE] (e.g. "rp6" for Redmi Pad Pro).
     */
    private val profiles = mapOf(
        "rp6" to DeviceThermalProfile(
            name = "Redmi Pad Pro (rp6)",
            // Add rp6-specific rules here when needed. They will be evaluated
            // before the generic rules above.
            additionalRules = emptyList(),
        ),
    )

    /** Returns a classifier for the given device codename. */
    fun classifierFor(device: String): ThermalClassifier {
        val profile = profiles.entries
            .find { device.matches(Regex(it.key, RegexOption.IGNORE_CASE)) }
            ?.value

        val rules = profile?.overrideRules ?: run {
            val additional = profile?.additionalRules ?: emptyList()
            additional + genericRules
        }
        return ThermalClassifier(rules)
    }

    /** Convenience classifier that only uses the generic rules. */
    val defaultClassifier: ThermalClassifier = ThermalClassifier(genericRules)
}
