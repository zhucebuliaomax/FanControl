package com.mmax.fancontrol.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mmax.fancontrol.RootAccessManager
import com.mmax.fancontrol.data.FanCurvePreferences
import com.mmax.fancontrol.data.Prefs
import com.mmax.fancontrol.tile.FanQuickSettingsTile
import com.mmax.fancontrol.tile.OverlayTileService

/**
 * Restores fan control after boot only when the user opted in.
 * The overlay is always left off after a reboot.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)

        // Overlay visibility is intentionally never restored across a reboot.
        prefs.edit().putBoolean(Prefs.OVERLAY_ENABLED, false).apply()
        OverlayTileService.requestRefresh(appContext)

        if (!prefs.getBoolean(Prefs.AUTO_START_ENABLED, false)) {
            FanCurvePreferences.select(prefs, null)
            FanQuickSettingsTile.requestRefresh(appContext)
            Log.i(TAG, "Boot detected — automatic start is disabled")
            pendingResult.finish()
            return
        }

        Log.i(TAG, "Boot detected — preparing automatic fan-control start")
        RootAccessManager.ensureRoot { granted ->
            val started = granted && runCatching {
                SystemControlService.startOrUpdate(appContext)
            }.onFailure { error ->
                Log.e(TAG, "Unable to start fan control after boot", error)
            }.isSuccess

            if (!started) {
                FanCurvePreferences.select(prefs, null)
                if (!granted) Log.w(TAG, "Root access unavailable after boot")
            }
            FanQuickSettingsTile.requestRefresh(appContext)
            OverlayTileService.requestRefresh(appContext)
            pendingResult.finish()
        }
    }
}
