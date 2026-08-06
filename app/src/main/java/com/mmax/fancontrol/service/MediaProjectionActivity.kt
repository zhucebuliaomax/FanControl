package com.mmax.retrocontrol.service

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

class MediaProjectionActivity : androidx.activity.ComponentActivity() {
    private val captureLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.takeIf { result.resultCode == RESULT_OK }?.let { token ->
            startForegroundService(
                Intent(this, SystemControlService::class.java)
                    .setAction(SystemControlService.ACTION_SET_PROJECTION_INTENT)
                    .putExtra(SystemControlService.EXTRA_PROJECTION_INTENT, token)
            )
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = getSystemService(MediaProjectionManager::class.java)
        captureLauncher.launch(manager.createScreenCaptureIntent())
    }
}
