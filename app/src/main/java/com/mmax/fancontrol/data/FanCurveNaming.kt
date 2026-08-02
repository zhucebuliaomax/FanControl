package com.mmax.retrocontrol.data

import android.content.Context
import androidx.annotation.StringRes
import com.mmax.retrocontrol.R

@get:StringRes
val BuiltInFanCurve.labelRes: Int
    get() = when (this) {
        BuiltInFanCurve.QUIET -> R.string.fan_mode_quiet
        BuiltInFanCurve.NORMAL -> R.string.fan_mode_normal
        BuiltInFanCurve.PERFORMANCE -> R.string.fan_mode_performance
        BuiltInFanCurve.LEGACY_CUSTOM -> R.string.fan_mode_custom
    }

fun FanCurveProfile.displayName(context: Context): String =
    customName ?: builtIn?.let { context.getString(it.labelRes) }
    ?: context.getString(R.string.unnamed_fan_curve)
