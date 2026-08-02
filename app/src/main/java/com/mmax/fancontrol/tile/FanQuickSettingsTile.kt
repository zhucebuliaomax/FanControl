package com.mmax.retrocontrol.tile

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.mmax.retrocontrol.R
import com.mmax.retrocontrol.RootAccessManager
import com.mmax.retrocontrol.data.FanControlConfig
import com.mmax.retrocontrol.data.FanCurvePreferences
import com.mmax.retrocontrol.data.Prefs
import com.mmax.retrocontrol.data.displayName
import com.mmax.retrocontrol.service.SystemControlService

/**
 * Standard third-party TileService supports one click target only:
 * tap toggles Off/on and long-press opens FanCurveTilePreferencesActivity.
 */
class FanQuickSettingsTile : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile(currentConfig())
    }

    override fun onClick() {
        super.onClick()
        val prefs = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        val next = FanCurvePreferences.toggle(prefs)
        updateTile(next)
        RootAccessManager.ensureRoot { granted ->
            val started = granted && runCatching {
                SystemControlService.startOrUpdate(applicationContext)
            }.isSuccess
            if (next.enabled && !started) {
                FanCurvePreferences.select(prefs, null)
            }
            requestRefresh(applicationContext)
        }
    }

    private fun currentConfig(): FanControlConfig =
        FanCurvePreferences.load(getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE))

    private fun updateTile(config: FanControlConfig) {
        qsTile?.apply {
            icon = Icon.createWithResource(applicationContext, R.drawable.ic_tile_fan)
            label = getString(R.string.tile_fan_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = config.activeProfile?.displayName(this@FanQuickSettingsTile)
                    ?: getString(R.string.fan_mode_off)
            }
            state = if (config.enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }

    companion object {
        fun requestRefresh(context: Context) {
            requestListeningState(context, ComponentName(context, FanQuickSettingsTile::class.java))
        }
    }
}
