package com.mmax.retrocontrol.service

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
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mmax.retrocontrol.MainActivity
import com.mmax.retrocontrol.R
import com.mmax.retrocontrol.data.FanCurvePreferences
import com.mmax.retrocontrol.data.AppProfilePreferences
import com.mmax.retrocontrol.data.FanSelectionPreferences
import com.mmax.retrocontrol.data.FanControlConfig
import com.mmax.retrocontrol.data.FanCurveSerializer
import com.mmax.retrocontrol.data.PerformanceProfilePreferences
import com.mmax.retrocontrol.data.PerformanceProfileResolver
import com.mmax.retrocontrol.data.PresetPreferences
import com.mmax.retrocontrol.data.Prefs
import com.mmax.retrocontrol.data.JoystickProfile
import com.mmax.retrocontrol.data.JoystickProfilePreferences
import com.mmax.retrocontrol.data.displayName
import com.mmax.retrocontrol.hardware.FanController
import com.mmax.retrocontrol.hardware.FanResponseController
import com.mmax.retrocontrol.hardware.CpuFrequencyController
import com.mmax.retrocontrol.hardware.JoystickEffectEngine
import com.mmax.retrocontrol.hardware.TelemetryRepository
import com.mmax.retrocontrol.hardware.ThermalSensorReader
import com.mmax.retrocontrol.hardware.ThermalSnapshot
import com.mmax.retrocontrol.overlay.TelemetryOverlay
import com.mmax.retrocontrol.tile.FanQuickSettingsTile
import com.mmax.retrocontrol.tile.OverlayTileService
import com.mmax.retrocontrol.tile.JoystickQuickSettingsTile
import com.mmax.retrocontrol.util.formatTemperature
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
 * The foreground service owns fan, joystick RGB, and CPU frequency-profile writes
 * plus telemetry polling.
 *
 * CPU writes are limited to cpufreq policy minimum/maximum nodes. It never changes
 * governors, GPU settings, refresh-rate settings, thermal-zone modes, or kernel
 * thermal protection.
 */
class SystemControlService : Service() {

    companion object {
        private const val TAG = "SystemControlService"
        const val CHANNEL_ID = "fan_control"
        private const val NOTIFICATION_ID = 1
        const val ACTION_UPDATE = "com.mmax.retrocontrol.UPDATE"
        const val ACTION_SET_PROJECTION_INTENT =
            "com.mmax.retrocontrol.SET_PROJECTION_INTENT"
        const val EXTRA_PROJECTION_INTENT = "projection_intent"

        fun startOrUpdate(context: Context) {
            context.startForegroundService(
                Intent(context, SystemControlService::class.java).setAction(ACTION_UPDATE)
            )
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var prefs: SharedPreferences

    private var fanJob: Job? = null
    private var foregroundJob: Job? = null
    private var screenOffJob: Job? = null
    private var performanceJob: Job? = null
    private var overlay: TelemetryOverlay? = null
    private lateinit var joystickEffects: JoystickEffectEngine
    private var lastNotificationUpdateMs = 0L
    private var screenReceiverRegistered = false

    @Volatile
    private var foregroundPackageName: String? = null

    @Volatile
    private var fanConfig = FanControlConfig()

    @Volatile
    private var joystickProfile: JoystickProfile? = null

    @Volatile
    private var configRevision = 0L

    @Volatile
    private var overlayEnabled = false

    @Volatile
    private var fanSuspendedForScreenOff = false

    @Volatile
    private var performanceRequestInitialized = false

    @Volatile
    private var lastRequestedPerformanceProfileId: String? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    scheduleScreenOffSuspend()
                    joystickEffects.suspendForScreenOff()
                }
                Intent.ACTION_USER_PRESENT -> {
                    resumeFanAfterUnlock()
                    joystickEffects.resumeAfterScreenOn()
                }
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
            Prefs.PRESET_CATALOG,
            Prefs.SELECTED_PRESET,
            Prefs.SELECTED_GAME_PROFILE,
            Prefs.SELECTED_NON_GAME_PROFILE,
            Prefs.APP_PROFILE_CATALOG,
            Prefs.FAN_SELECTION_SOURCE,
            Prefs.FAN_SELECTION_CURVE,
            Prefs.FAN_TILE_ENABLED -> {
                loadFanPreferences()
                loadJoystickPreferences()
                applyPerformanceProfile()
            }
            Prefs.PERFORMANCE_PROFILE_CATALOG -> applyPerformanceProfile(force = true)
            Prefs.JOYSTICK_PROFILE_CATALOG -> {
                loadJoystickPreferences()
                JoystickQuickSettingsTile.requestRefresh(applicationContext)
            }
            Prefs.JOYSTICK_SELECTION_SOURCE,
            Prefs.JOYSTICK_SELECTION_PROFILE,
            Prefs.JOYSTICK_TILE_ENABLED -> {
                loadJoystickPreferences(force = true)
                JoystickQuickSettingsTile.requestRefresh(applicationContext)
            }
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
        joystickEffects = JoystickEffectEngine(applicationContext, scope)
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

        startOrdinaryForeground()
        loadJoystickPreferences()
        applyPerformanceProfile(force = true)
        startForegroundAppMonitor()
        startFanLoop()
        applyOverlayState()
        FanQuickSettingsTile.requestRefresh(applicationContext)
        JoystickQuickSettingsTile.requestRefresh(applicationContext)
        OverlayTileService.requestRefresh(applicationContext)
        if (!getSystemService(PowerManager::class.java).isInteractive) {
            scheduleScreenOffSuspend()
            joystickEffects.suspendForScreenOff()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE -> {
                loadFanPreferences()
                loadJoystickPreferences(force = true)
                applyPerformanceProfile(force = true)
            }
            ACTION_SET_PROJECTION_INTENT -> {
                val token = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_PROJECTION_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_PROJECTION_INTENT)
                }
                if (token != null) {
                    promoteForMediaProjection()
                    joystickEffects.setMediaProjectionIntent(token)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        if (screenReceiverRegistered) {
            unregisterReceiver(screenReceiver)
            screenReceiverRegistered = false
        }
        screenOffJob?.cancel()
        foregroundJob?.cancel()
        performanceJob?.cancel()
        overlay?.hide()
        overlay = null
        joystickEffects.destroy()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun loadFanPreferences() {
        fanConfig = FanSelectionPreferences.resolveEffectiveConfig(
            prefs = prefs,
            suppliedFanConfig = FanCurvePreferences.load(prefs),
            foregroundPackageName = foregroundPackageName,
            foregroundIsGame = AppProfilePreferences.isGame(this, foregroundPackageName),
        )
        configRevision++
    }

    private fun loadJoystickPreferences(force: Boolean = false) {
        joystickProfile = JoystickProfilePreferences.resolveEffectiveProfile(
            prefs = prefs,
            foregroundPackageName = foregroundPackageName,
            foregroundIsGame = AppProfilePreferences.isGame(this, foregroundPackageName),
        )
        joystickEffects.apply(joystickProfile, force = force)
    }

    private fun loadOverlayPreference() {
        overlayEnabled = prefs.getBoolean(Prefs.OVERLAY_ENABLED, false)
    }

    private fun applyPerformanceProfile(force: Boolean = false) {
        performanceJob?.cancel()
        performanceJob = scope.launch {
            val policies = CpuFrequencyController.detectPolicies()
            if (policies.isEmpty()) {
                Log.w(TAG, "CPU frequency policies are unavailable")
                return@launch
            }
            val profileConfig = PerformanceProfilePreferences.load(prefs, policies)
            val performanceIds = profileConfig.profiles.mapTo(mutableSetOf()) { it.id }
            val fanIds = FanCurvePreferences.load(prefs).catalog.profiles
                .mapTo(mutableSetOf()) { it.id }
            val joystickIds = JoystickProfilePreferences.load(prefs).profiles
                .mapTo(mutableSetOf()) { it.id }
            val presetConfig = PresetPreferences.load(
                prefs,
                fanIds,
                joystickIds,
                performanceIds,
            )
            val appProfiles = AppProfilePreferences.load(
                prefs = prefs,
                availablePresetIds = presetConfig.catalog.presets
                    .mapTo(mutableSetOf()) { it.id },
                availableFanCurveIds = fanIds,
                availableJoystickProfileIds = joystickIds,
                availablePerformanceProfileIds = performanceIds,
            )
            val appIsGame = AppProfilePreferences.isGame(this@SystemControlService, foregroundPackageName)
            val targetId = PerformanceProfileResolver.resolveTargetProfileId(
                profileConfig = profileConfig,
                presetConfig = presetConfig,
                appProfile = foregroundPackageName?.let(appProfiles::get),
                appIsGame = appIsGame,
            )
            val previouslyApplied = prefs.getString(
                Prefs.LAST_APPLIED_PERFORMANCE_PROFILE,
                null,
            )
            if (
                !force && performanceRequestInitialized &&
                targetId == lastRequestedPerformanceProfileId
            ) {
                return@launch
            }

            val target = if (targetId == null) {
                if (previouslyApplied == null) {
                    performanceRequestInitialized = true
                    lastRequestedPerformanceProfileId = null
                    return@launch
                }
                profileConfig.stockProfile
            } else {
                profileConfig.profile(targetId)
            } ?: return@launch

            CpuFrequencyController.applyProfile(target, policies)
                .onSuccess { result ->
                    if (!result.verificationPassed) {
                        performanceRequestInitialized = false
                        return@onSuccess
                    }
                    performanceRequestInitialized = true
                    lastRequestedPerformanceProfileId = targetId
                    prefs.edit().apply {
                        if (targetId == null) {
                            remove(Prefs.LAST_APPLIED_PERFORMANCE_PROFILE)
                        } else {
                            putString(Prefs.LAST_APPLIED_PERFORMANCE_PROFILE, targetId)
                        }
                    }.apply()
                }
                .onFailure { error ->
                    performanceRequestInitialized = false
                    Log.e(TAG, "Unable to apply performance profile ${target.id}", error)
                }
        }
    }

    private fun startForegroundAppMonitor() {
        foregroundJob?.cancel()
        foregroundJob = scope.launch {
            while (isActive) {
                val foreground = ForegroundAppResolver.currentPackageName()
                if (foreground != foregroundPackageName) {
                    foregroundPackageName = foreground
                    loadFanPreferences()
                    loadJoystickPreferences()
                    applyPerformanceProfile()
                    Log.i(
                        TAG,
                        "Foreground changed: package=$foreground, " +
                            "fan=${fanConfig.activeProfile?.id ?: "off"}, " +
                            "joystick=${joystickProfile?.id ?: "off"}, " +
                            "performance=${lastRequestedPerformanceProfileId ?: "unmanaged"}",
                    )
                }
                delay(1_000L)
            }
        }
    }

    private fun promoteForMediaProjection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        }
    }

    /** Ordinary fan/RGB operation must not claim MediaProjection before consent. */
    private fun startOrdinaryForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
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
                val controlTemp = thermal.controlTempC

                val output = when {
                    fanSuspendedForScreenOff || profile == null -> {
                        if (configChanged) {
                            response.resetImmediate(controlTemp, 0.0, now)
                        }
                        0.0
                    }
                    controlTemp <= 0.0 -> null
                    configChanged -> {
                        val immediate = curvePercent(profile.points, controlTemp)
                        response.resetImmediate(controlTemp, immediate, now)
                    }
                    else -> response.update(controlTemp, now) { temp ->
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
                            controlTemp > 0.0,
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
                            controlTemp > 0.0,
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
        points: List<com.mmax.retrocontrol.data.FanCurvePoint>,
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
            val controlTemp = TelemetryRepository.state.value.thermal.controlTempC
            if (profile == null || controlTemp <= 0.0) return@launch
            runCatching {
                FanCurvePreferences.adjustAroundTemperature(
                    prefs = prefs,
                    profileId = profile.id,
                    tempC = controlTemp,
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
