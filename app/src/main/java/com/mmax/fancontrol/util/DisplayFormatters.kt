package com.mmax.fancontrol.util

import java.text.NumberFormat
import java.util.Locale

fun formatTemperature(value: Double): String {
    val number = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
        isGroupingUsed = false
    }
    return "${number.format(value)}℃"
}

fun formatFanPercent(percent: Int): String = "${percent.coerceIn(0, 100)}%"
