package com.mmax.fancontrol.tile

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.os.Build
import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.mmax.fancontrol.R
import com.mmax.fancontrol.data.Prefs
import com.mmax.fancontrol.service.SystemControlService

class OverlayTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile(isActuallyEnabled())
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        if (!Settings.canDrawOverlays(this)) {
            val permission = Intent(this, OverlayPermissionActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    41,
                    permission,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(permission)
            }
            updateTile(false)
            return
        }

        val next = !isActuallyEnabled()
        getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(Prefs.OVERLAY_ENABLED, next)
            .apply()
        updateTile(next)
        val started = runCatching {
            SystemControlService.startOrUpdate(applicationContext)
        }.isSuccess
        if (next && !started) {
            getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(Prefs.OVERLAY_ENABLED, false)
                .apply()
            updateTile(false)
        }
    }

    private fun isActuallyEnabled(): Boolean =
        Settings.canDrawOverlays(this) &&
            getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
                .getBoolean(Prefs.OVERLAY_ENABLED, false)

    private fun updateTile(enabled: Boolean) {
        qsTile?.apply {
            icon = Icon.createWithResource(applicationContext, R.drawable.ic_tile_overlay)
            label = getString(R.string.tile_overlay_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = if (enabled) {
                    getString(R.string.tile_on)
                } else {
                    getString(R.string.tile_off)
                }
            }
            state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }

    companion object {
        fun requestRefresh(context: Context) {
            requestListeningState(context, ComponentName(context, OverlayTileService::class.java))
        }
    }
}
