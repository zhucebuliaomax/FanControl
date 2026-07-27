package com.mmax.fancontrol.tile

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.mmax.fancontrol.R
import com.mmax.fancontrol.RootAccessManager
import com.mmax.fancontrol.data.FanControlConfig
import com.mmax.fancontrol.data.FanCurvePreferences
import com.mmax.fancontrol.data.Prefs
import com.mmax.fancontrol.data.displayName
import com.mmax.fancontrol.service.SystemControlService

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
        RootAccessManager.ensureRoot {
            SystemControlService.startOrUpdate(applicationContext)
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
