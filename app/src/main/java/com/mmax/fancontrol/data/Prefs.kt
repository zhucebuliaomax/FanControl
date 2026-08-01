package com.mmax.fancontrol.data

object Prefs {
    const val FILE = "fan_control"

    /** "OFF" or an active profile id. Kept under the legacy key for migration compatibility. */
    const val FAN_MODE = "fan_mode"
    const val LAST_FAN_CURVE = "last_fan_curve"
    const val FAN_CURVE_CATALOG = "fan_curve_catalog_v2"

    // Version 1 migration keys.
    const val FAN_CURVE_QUIET = "fan_curve_quiet"
    const val FAN_CURVE_NORMAL = "fan_curve_normal"
    const val FAN_CURVE_PERFORMANCE = "fan_curve_performance"
    const val FAN_CURVE_CUSTOM = "fan_curve_custom"
    const val LEGACY_FAN_CURVE_CUSTOM = "fan_curve_points"

    const val OVERLAY_ENABLED = "overlay_enabled"
    const val OVERLAY_X = "overlay_x"
    const val OVERLAY_Y = "overlay_y"
    const val OVERLAY_DISPLAY_MODE = "overlay_display_mode"
    const val AUTO_START_ENABLED = "auto_start_enabled"
    const val NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
    const val ROOT_NOTICE_ACKNOWLEDGED = "root_notice_acknowledged"
}
