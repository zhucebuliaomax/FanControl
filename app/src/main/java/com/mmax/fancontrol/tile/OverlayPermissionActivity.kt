package com.mmax.fancontrol.tile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.mmax.fancontrol.data.Prefs
import com.mmax.fancontrol.service.SystemControlService

/**
 * Visible launch trampoline for overlay permission. TileService cannot safely
 * launch the old settings Intent directly on Android 14 and newer.
 */
class OverlayPermissionActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        finishPermissionFlow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Settings.canDrawOverlays(this)) {
            enableOverlayAndFinish()
            return
        }
        permissionLauncher.launch(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            )
        )
    }

    private fun finishPermissionFlow() {
        if (Settings.canDrawOverlays(this)) {
            enableOverlayAndFinish()
        } else {
            getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(Prefs.OVERLAY_ENABLED, false)
                .apply()
            OverlayTileService.requestRefresh(this)
            finish()
        }
    }

    private fun enableOverlayAndFinish() {
        getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(Prefs.OVERLAY_ENABLED, true)
            .apply()
        SystemControlService.startOrUpdate(this)
        OverlayTileService.requestRefresh(this)
        finish()
    }
}
