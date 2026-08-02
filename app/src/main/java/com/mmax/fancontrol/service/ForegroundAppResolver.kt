package com.mmax.retrocontrol.service

import com.topjohnwu.superuser.Shell

/** Reads Android's resumed activity through the already-required root shell. */
object ForegroundAppResolver {
    private val componentPattern = Regex(
        "(?:u\\d+\\s+)?([A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+)/[A-Za-z0-9_.$]+"
    )

    fun currentPackageName(): String? {
        val activityLine = Shell.cmd(
            "dumpsys activity activities | grep -m 1 mResumedActivity"
        ).exec().out.firstOrNull()
        val windowLine = if (activityLine == null) {
            Shell.cmd("dumpsys window windows | grep -m 1 mCurrentFocus")
                .exec().out.firstOrNull()
        } else {
            null
        }
        return componentPattern.find(activityLine ?: windowLine.orEmpty())?.groupValues?.get(1)
    }
}
