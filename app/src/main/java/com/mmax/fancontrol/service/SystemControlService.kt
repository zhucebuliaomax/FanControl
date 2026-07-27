package com.mmax.fancontrol.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mmax.fancontrol.MainActivity
import com.mmax.fancontrol.R
import com.mmax.fancontrol.data.FanCurvePreferences
import com.mmax.fancontrol.data.FanControlConfig
import com.mmax.fancontrol.data.FanCurveSerializer
import com.mmax.fancontrol.data.Prefs
import com.mmax.fancontrol.data.displayName
import com.mmax.fancontrol.hardware.FanController
import com.mmax.fancontrol.hardware.FanResponseController
import com.mmax.fancontrol.hardware.TelemetryRepository
import com.mmax.fancontrol.hardware.ThermalSensorReader
import com.mmax.fancontrol.hardware.ThermalSnapshot
import com.mmax.fancontrol.overlay.TelemetryOverlay
import com.mmax.fancontrol.tile.FanQuickSettingsTile
import com.mmax.fancontrol.tile.OverlayTileService
import com.mmax.fancontrol.util.formatTemperature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The foreground service owns fan writes and telemetry polling.
 *
 * It never writes CPU/GPU settings, refresh-rate settings, thermal-zone modes
 * or kernel thermal protection. Hardware writes are limited to pwm-fan pwm1,
 * with cur_state as a compatibility fallback.
 */
class SystemControlService : Service() {

    companion object {
        const val CHANNEL_ID = "fan_control"
        private const val NOTIFICATION_ID = 1
        const val ACTION_UPDATE = "com.mmax.fancontrol.UPDATE"

        fun startOrUpdate(context: Context) {
            context.startForegroundService(
                Intent(context, SystemControlService::class.java).setAction(ACTION_UPDATE)
            )
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var prefs: SharedPreferences

    private var fanJob: Job? = null
    private var screenOffJob: Job? = null
    private var overlay: TelemetryOverlay? = null
    private var lastNotificationUpdateMs = 0L
    private var screenReceiverRegistered = false

    @Volatile
    private var fanConfig = FanControlConfig()

    @Volatile
    private var configRevision = 0L

    @Volatile
    private var overlayEnabled = false

    @Volatile
    private var fanSuspendedForScreenOff = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> scheduleScreenOffSuspend()
                Intent.ACTION_USER_PRESENT -> resumeFanAfterUnlock()
            }
        }
    }

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            Prefs.FAN_MODE,
            Prefs.FAN_CURVE_CATALOG,
            Prefs.FAN_CURVE_QUIET,
            Prefs.FAN_CURVE_NORMAL,
            Prefs.FAN_CURVE_PERFORMANCE,
            Prefs.FAN_CURVE_CUSTOM,
            Prefs.LEGACY_FAN_CURVE_CUSTOM -> loadFanPreferences()
            Prefs.OVERLAY_ENABLED -> {
                loadOverlayPreference()
                applyOverlayState()
                OverlayTileService.requestRefresh(applicationContext)
            }
        }
        if (key == Prefs.FAN_MODE) FanQuickSettingsTile.requestRefresh(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        createNotificationChannel()
        loadFanPreferences()
        loadOverlayPreference()
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
        screenReceiverRegistered = true

        startForeground(NOTIFICATION_ID, buildNotification())
        startFanLoop()
        applyOverlayState()
        if (!getSystemService(PowerManager::class.java).isInteractive) {
            scheduleScreenOffSuspend()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        if (screenReceiverRegistered) {
            unregisterReceiver(screenReceiver)
            screenReceiverRegistered = false
        }
        screenOffJob?.cancel()
        overlay?.hide()
        overlay = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun loadFanPreferences() {
        fanConfig = FanCurvePreferences.load(prefs)
        configRevision++
    }

    private fun loadOverlayPreference() {
        overlayEnabled = prefs.getBoolean(Prefs.OVERLAY_ENABLED, false)
    }

    private fun startFanLoop() {
        fanJob?.cancel()
        fanJob = scope.launch {
            val response = FanResponseController()
            var appliedRevision = Long.MIN_VALUE
            var thermal = ThermalSnapshot()
            var lastThermalReadMs = 0L

            while (isActive) {
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - lastThermalReadMs >= 500L) {
                    thermal = ThermalSensorReader.read()
                    lastThermalReadMs = now
                }

                val config = fanConfig
                val profile = config.activeProfile
                val revision = configRevision
                val configChanged = revision != appliedRevision
                val averageTemp = thermal.computeSummary.averageC

                val output = when {
                    fanSuspendedForScreenOff || profile == null -> {
                        if (configChanged) {
                            response.resetImmediate(averageTemp, 0.0, now)
                        }
                        0.0
                    }
                    averageTemp <= 0.0 -> null
                    configChanged -> {
                        val immediate = curvePercent(profile.points, averageTemp)
                        response.resetImmediate(averageTemp, immediate, now)
                    }
                    else -> response.update(averageTemp, now) { temp ->
                        curvePercent(profile.points, temp)
                    }
                }

                val profileName = profile?.displayName(this@SystemControlService).orEmpty()
                val profilePoints = profile?.points.orEmpty()
                if (output != null) {
                    val percent = FanController.writePercent(output)
                    TelemetryRepository.updateThermal(
                        thermal = thermal,
                        fanPercent = percent,
                        fanAdjustEnabled = profile != null &&
                            !fanSuspendedForScreenOff &&
                            averageTemp > 0.0,
                        activeCurveName = profileName,
                        activeCurvePoints = profilePoints,
                    )
                } else {
                    val maxState = FanController.readMaxState().coerceAtLeast(1)
                    val percent = (FanController.readCurState().toDouble() / maxState * 100.0)
                        .roundToInt()
                    TelemetryRepository.updateThermal(
                        thermal = thermal,
                        fanPercent = percent,
                        fanAdjustEnabled = profile != null &&
                            !fanSuspendedForScreenOff &&
                            averageTemp > 0.0,
                        activeCurveName = profileName,
                        activeCurvePoints = profilePoints,
                    )
                }

                if (now - lastNotificationUpdateMs >= 2_000L) {
                    updateNotification()
                    lastNotificationUpdateMs = now
                }

                appliedRevision = revision
                delay(300L)
            }
        }
    }

    private fun curvePercent(
        points: List<com.mmax.fancontrol.data.FanCurvePoint>,
        tempC: Double,
    ): Double {
        return FanCurveSerializer.interpolate(tempC, points)
    }

    private fun applyOverlayState() {
        if (overlayEnabled && android.provider.Settings.canDrawOverlays(this)) {
            if (overlay == null) {
                overlay = TelemetryOverlay(
                    context = applicationContext,
                    onAdjustFan = ::adjustActiveCurve,
                )
            }
            overlay?.show()
        } else {
            overlay?.hide()
            overlay = null
        }
    }

    private fun adjustActiveCurve(deltaPercent: Int) {
        scope.launch {
            val profile = fanConfig.activeProfile
            val averageTemp = TelemetryRepository.state.value.thermal.computeSummary.averageC
            if (profile == null || averageTemp <= 0.0) return@launch
            runCatching {
                FanCurvePreferences.adjustAroundTemperature(
                    prefs = prefs,
                    profileId = profile.id,
                    tempC = averageTemp,
                    deltaPercent = deltaPercent,
                )
            }
        }
    }

    private fun scheduleScreenOffSuspend() {
        screenOffJob?.cancel()
        screenOffJob = scope.launch {
            delay(5_000L)
            if (!fanSuspendedForScreenOff) {
                fanSuspendedForScreenOff = true
                configRevision++
            }
        }
    }

    private fun resumeFanAfterUnlock() {
        screenOffJob?.cancel()
        screenOffJob = null
        if (fanSuspendedForScreenOff) {
            fanSuspendedForScreenOff = false
            configRevision++
        }
    }

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
        )
    }

    private fun buildNotification(): Notification {
        val telemetry = TelemetryRepository.state.value
        val profile = fanConfig.activeProfile
        val cpu = telemetry.thermal.cpuSummary
        val gpu = telemetry.thermal.gpuSummary
        val cpuText = if (cpu.count > 0) formatTemperature(cpu.averageC)
            else getString(R.string.not_available)
        val gpuText = if (gpu.count > 0) formatTemperature(gpu.averageC)
            else getString(R.string.not_available)
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_fan)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(
                getString(
                    R.string.notification_content,
                    profile?.displayName(this) ?: getString(R.string.fan_mode_off),
                    telemetry.fanPercent,
                    cpuText,
                    gpuText,
                )
            )
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification())
    }
}
