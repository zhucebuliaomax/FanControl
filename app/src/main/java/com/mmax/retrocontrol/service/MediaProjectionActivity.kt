package com.mmax.retrocontrol.service

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import java.util.concurrent.atomic.AtomicBoolean

class MediaProjectionActivity : androidx.activity.ComponentActivity() {
    private var ownsCaptureRequest = false

    private val captureLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        captureRequestInFlight.set(false)
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
        if (savedInstanceState != null && captureRequestInFlight.get()) {
            ownsCaptureRequest = true
            return
        }
        if (!captureRequestInFlight.compareAndSet(false, true)) {
            finish()
            return
        }
        ownsCaptureRequest = true
        val manager = getSystemService(MediaProjectionManager::class.java)
        runCatching { captureLauncher.launch(manager.createScreenCaptureIntent()) }
            .onFailure {
                captureRequestInFlight.set(false)
                finish()
            }
    }

    override fun onDestroy() {
        if (ownsCaptureRequest && isFinishing) captureRequestInFlight.set(false)
        super.onDestroy()
    }

    companion object {
        private val captureRequestInFlight = AtomicBoolean(false)

        fun createIntent(context: Context): Intent =
            Intent(context, MediaProjectionActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
    }
}
