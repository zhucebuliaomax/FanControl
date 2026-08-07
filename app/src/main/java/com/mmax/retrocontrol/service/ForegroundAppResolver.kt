package com.mmax.retrocontrol.service

import com.topjohnwu.superuser.Shell

/** Reads Android's resumed activity through the already-required root shell. */
object ForegroundAppResolver {
    private val componentPattern = Regex(
        "(?:u\\d+\\s+)?([A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+)/[A-Za-z0-9_.$]+"
    )

    fun currentPackageName(): String? {
        val activityLines = Shell.cmd("dumpsys activity activities").exec().out
        val activityLine = activityFieldPriority.firstNotNullOfOrNull { field ->
            activityLines.firstOrNull { field in it }
        }
        componentPattern.find(activityLine.orEmpty())?.let { match ->
            return match.groupValues[1]
        }

        val windowLines = Shell.cmd("dumpsys window windows").exec().out
        val windowLine = windowFieldPriority.firstNotNullOfOrNull { field ->
            windowLines.firstOrNull { field in it }
        }
        return componentPattern.find(windowLine.orEmpty())?.groupValues?.get(1)
    }

    private val activityFieldPriority = listOf(
        "topResumedActivity=",
        "mResumedActivity:",
        "ResumedActivity:",
    )

    private val windowFieldPriority = listOf(
        "mCurrentFocus=",
        "mFocusedApp=",
    )
}
