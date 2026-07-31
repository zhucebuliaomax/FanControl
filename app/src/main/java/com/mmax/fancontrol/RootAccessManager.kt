package com.mmax.fancontrol

import android.os.Handler
import android.os.Looper
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Serializes root-shell acquisition so one app entry cannot trigger duplicate requests. */
object RootAccessManager {
    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val callbacks = mutableListOf<(Boolean) -> Unit>()
    private val mutableHasRoot = MutableStateFlow(false)
    val hasRoot: StateFlow<Boolean> = mutableHasRoot.asStateFlow()

    @Volatile
    private var requestInFlight = false

    fun ensureRoot(
        forceRefresh: Boolean = false,
        onResult: (Boolean) -> Unit = {},
    ) {
        val cached = runCatching { Shell.getCachedShell() }.getOrNull()
        if (!forceRefresh && cached?.isAlive == true && cached.isRoot) {
            mainHandler.post {
                mutableHasRoot.value = true
                onResult(true)
            }
            return
        }

        synchronized(lock) {
            callbacks += onResult
            if (requestInFlight) return
            requestInFlight = true
        }

        Shell.EXECUTOR.execute {
            val shellToReplace = runCatching {
                Shell.getCachedShell()?.takeIf {
                    it.isAlive && (forceRefresh || !it.isRoot)
                }
            }.getOrNull()
            if (shellToReplace != null) {
                runCatching { shellToReplace.close() }
                .also { runCatching { Shell.getCachedShell() } }
            }
            Shell.getShell(Shell.EXECUTOR) { shell ->
                complete(shell.isAlive && shell.isRoot)
            }
        }
    }

    private fun complete(granted: Boolean) {
        val waiting = synchronized(lock) {
            requestInFlight = false
            callbacks.toList().also { callbacks.clear() }
        }
        mainHandler.post {
            mutableHasRoot.value = granted
            waiting.forEach { callback -> callback(granted) }
        }
    }
}
