@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.mmax.retrocontrol.ui

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import android.os.Build
import android.os.SystemClock
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mmax.retrocontrol.R
import com.mmax.retrocontrol.BuildConfig
import com.mmax.retrocontrol.RootAccessManager
import com.mmax.retrocontrol.data.FanCurveJson
import com.mmax.retrocontrol.data.FanCurvePoint
import com.mmax.retrocontrol.data.AppProfilePreferences
import com.mmax.retrocontrol.data.displayName
import com.mmax.retrocontrol.designsystem.FocusScrollMargin
import com.mmax.retrocontrol.designsystem.bringIntoViewOnFocus
import com.mmax.retrocontrol.feature.authorization.AuthorizationManagementSection
import com.mmax.retrocontrol.feature.authorization.AuthorizationUiState
import com.mmax.retrocontrol.feature.fan.FanProfilesAddCurveButton
import com.mmax.retrocontrol.feature.fan.FanProfileItemUiState
import com.mmax.retrocontrol.feature.fan.FanProfileSectionState
import com.mmax.retrocontrol.feature.fan.FanProfilesSection
import com.mmax.retrocontrol.feature.fan.FanTelemetrySection
import com.mmax.retrocontrol.feature.fan.FanTelemetrySectionState
import com.mmax.retrocontrol.feature.fan.TemperatureTileUiState
import com.mmax.retrocontrol.feature.joystick.AddJoystickProfileButton
import com.mmax.retrocontrol.feature.joystick.JoystickProfileEditorDialog
import com.mmax.retrocontrol.feature.joystick.JoystickProfileUiState
import com.mmax.retrocontrol.feature.joystick.JoystickProfilesSection
import com.mmax.retrocontrol.feature.joystick.JoystickRgbMode
import com.mmax.retrocontrol.feature.joystick.R as JoystickR
import com.mmax.retrocontrol.hardware.TemperatureSummary
import com.mmax.retrocontrol.hardware.ThermalKind
import com.mmax.retrocontrol.hardware.ThermalReading
import com.mmax.retrocontrol.service.SystemControlService
import com.mmax.retrocontrol.service.MediaProjectionActivity
import com.mmax.retrocontrol.tile.OverlayPermissionActivity
import com.mmax.retrocontrol.util.formatFanPercent
import com.mmax.retrocontrol.util.formatTemperature
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vm: DashboardViewModel = viewModel(),
    onFanCurveSelected: (Boolean) -> Unit = {},
    onRefreshRoot: () -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val hasRoot by RootAccessManager.hasRoot.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var microphoneGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> microphoneGranted = granted }
    val lifecycleOwner = LocalLifecycleOwner.current
    var editingProfileId by remember { mutableStateOf<String?>(null) }
    var restoreFanCurveFocusId by remember { mutableStateOf<String?>(null) }
    var editingPresetId by remember { mutableStateOf<String?>(null) }
    var restorePresetFocusId by remember { mutableStateOf<String?>(null) }
    var editingJoystickProfileId by remember { mutableStateOf<String?>(null) }
    var restoreJoystickFocusId by remember { mutableStateOf<String?>(null) }
    var editingPerformanceProfileId by remember { mutableStateOf<String?>(null) }
    var restorePerformanceFocusId by remember { mutableStateOf<String?>(null) }
    var detailKind by remember { mutableStateOf<ThermalKind?>(null) }
    var selectedDestination by rememberSaveable {
        mutableStateOf(DashboardDestination.TELEMETRY)
    }
    var selectedControl by rememberSaveable { mutableStateOf<ControlModule?>(null) }
    var selectedApp by rememberSaveable { mutableStateOf<String?>(null) }
    var lastBackPressedAt by remember { mutableLongStateOf(0L) }
    var notificationsEnabled by remember {
        mutableStateOf(context.areFanNotificationsEnabled())
    }
    var overlayPermissionGranted by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }
    val fanProfileIds = state.fanConfig.catalog.visibleProfiles.map { it.id }
    val fanProfileFocusRequesters = remember(fanProfileIds) {
        List(fanProfileIds.size + 1) { FocusRequester() }
    }
    val addCurveFocusRequester = remember { FocusRequester() }
    val joystickProfileIds = state.joystickProfiles.visibleProfiles.map { it.id }
    val joystickProfileFocusRequesters = remember(joystickProfileIds) {
        List(joystickProfileIds.size + 1) { FocusRequester() }
    }
    val addJoystickProfileFocusRequester = remember { FocusRequester() }
    val performanceProfileIds = state.performanceProfiles.profiles.map { it.id }
    val performanceProfileFocusRequesters = remember(performanceProfileIds) {
        List(performanceProfileIds.size) { FocusRequester() }
    }
    val addPerformanceProfileFocusRequester = remember { FocusRequester() }
    val presetIds = state.presetConfig.catalog.presets.map { it.id }
    val presetFocusRequesters = remember(presetIds) {
        List(presetIds.size) { FocusRequester() }
    }
    val globalPresetFocusRequesters = remember(presetIds) {
        List(presetIds.size) { FocusRequester() }
    }
    val addPresetFocusRequester = remember { FocusRequester() }
    val appFocusRequester = remember { FocusRequester() }
    val overlayFocusRequester = remember { FocusRequester() }
    val telemetryFocusRequester = remember { FocusRequester() }
    val authorizationFocusRequesters = remember { List(8) { FocusRequester() } }
    val githubFocusRequester = remember { FocusRequester() }
    val navigationFocusRequesters = remember {
        List(DashboardDestination.entries.size) { FocusRequester() }
    }
    val controlFocusRequesters = remember {
        List(ControlModule.entries.size) { FocusRequester() }
    }
    val emptyDetailFocusRequester = remember { FocusRequester() }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = context.areFanNotificationsEnabled()
                overlayPermissionGranted = Settings.canDrawOverlays(context)
                microphoneGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val thermal = state.telemetry.thermal
    val unavailable = stringResource(R.string.not_available)
    val pressBackAgainToExit = stringResource(R.string.press_back_again_to_exit)
    val offName = stringResource(R.string.fan_mode_off)
    val joystickOffName = stringResource(JoystickR.string.joystick_off)
    fun fanCurveName(profileId: String?): String = profileId
        ?.let { state.fanConfig.catalog.profile(it)?.displayName(context) }
        ?: offName
    fun joystickProfileName(profileId: String?): String = profileId
        ?.let { state.joystickProfiles.profile(it)?.name }
        ?: joystickOffName
    val unmanagedPerformanceName = stringResource(R.string.performance_unmanaged)
    fun performanceProfileName(profileId: String?): String = profileId
        ?.let { state.performanceProfiles.profile(it)?.displayName(context) }
        ?: unmanagedPerformanceName
    fun joystickProfileUi(profileId: String): JoystickProfileUiState? =
        state.joystickProfiles.profile(profileId)?.let { profile ->
            JoystickProfileUiState(
                id = profile.id,
                name = profile.name,
                mode = profile.mode,
                red = profile.red,
                green = profile.green,
                blue = profile.blue,
                brightness = profile.brightness,
            )
        }
    val presetItems = state.presetConfig.catalog.presets.map { preset ->
        PresetListItemUiState(
            id = preset.id,
            name = preset.name,
            isDefault = preset.isDefault,
            fanCurveName = fanCurveName(preset.fanCurveId),
            joystickProfileName = joystickProfileName(preset.joystickId),
            performanceProfileName = performanceProfileName(preset.performanceProfileId),
        )
    }
    val appsWithProfileSummaries = state.installedApps.map { app ->
        val profile = state.appProfiles[app.packageName]
        val effectivePreset = AppProfilePreferences.effectivePreset(
            profile = profile,
            presetConfig = state.presetConfig,
            isGame = app.isGame,
        )
        val summary = buildList {
            add(effectivePreset.name)
            profile?.fanCurveId?.let { add(fanCurveName(it)) }
            profile?.joystickId?.let { add(joystickProfileName(it)) }
            profile?.buttonLayoutId?.let(::add)
            profile?.performanceProfileId?.let { add(performanceProfileName(it)) }
        }.joinToString(" · ")
        app.copy(profileSummary = summary)
    }

    BackHandler {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBackPressedAt <= 2_000L) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressedAt = now
            Toast.makeText(
                context,
                pressBackAgainToExit,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    LaunchedEffect(
        editingProfileId,
        restoreFanCurveFocusId,
        fanProfileIds,
    ) {
        val restoreId = restoreFanCurveFocusId
        if (editingProfileId == null && restoreId != null) {
            withFrameNanos { }
            val selectedIndex = fanProfileIds
                .indexOf(restoreId)
                .takeIf { it >= 0 }
                ?.plus(1)
                ?: 0
            fanProfileFocusRequesters[selectedIndex].requestFocus()
            restoreFanCurveFocusId = null
        }
    }

    LaunchedEffect(
        editingPerformanceProfileId,
        restorePerformanceFocusId,
        performanceProfileIds,
    ) {
        val restoreId = restorePerformanceFocusId
        if (editingPerformanceProfileId == null && restoreId != null) {
            withFrameNanos { }
            performanceProfileIds.indexOf(restoreId)
                .takeIf { it >= 0 }
                ?.let { performanceProfileFocusRequesters[it].requestFocus() }
            restorePerformanceFocusId = null
        }
    }

    LaunchedEffect(editingPresetId, restorePresetFocusId, presetIds) {
        val restoreId = restorePresetFocusId
        if (editingPresetId == null && restoreId != null) {
            withFrameNanos { }
            val index = presetIds.indexOf(restoreId).takeIf { it >= 0 } ?: 0
            presetFocusRequesters[index].requestFocus()
            restorePresetFocusId = null
        }
    }

    LaunchedEffect(
        editingJoystickProfileId,
        restoreJoystickFocusId,
        joystickProfileIds,
    ) {
        val restoreId = restoreJoystickFocusId
        if (editingJoystickProfileId == null && restoreId != null) {
            withFrameNanos { }
            val index = joystickProfileIds.indexOf(restoreId)
                .takeIf { it >= 0 }
                ?.plus(1)
                ?: 0
            joystickProfileFocusRequesters[index].requestFocus()
            restoreJoystickFocusId = null
        }
    }

    fun temperatureUi(summary: TemperatureSummary): TemperatureTileUiState =
        TemperatureTileUiState(
            average = if (summary.count > 0) {
                formatTemperature(summary.averageC)
            } else {
                unavailable
            },
            hottest = if (summary.count > 0) {
                "${summary.hottest?.name ?: unavailable}  ${formatTemperature(summary.maxC)}"
            } else {
                unavailable
            },
        )

    AdaptiveDashboardScaffold(
        selectedDestination = selectedDestination,
        onDestinationSelected = {
            selectedDestination = it
            selectedControl = null
            selectedApp = null
        },
        selectedControl = selectedControl,
        onControlSelected = { selectedControl = it },
        selectedApp = selectedApp,
        installedApps = appsWithProfileSummaries,
        onAppSelected = { selectedApp = it },
        navigationFocusRequesters = navigationFocusRequesters,
        controlFocusRequesters = controlFocusRequesters,
        appFocusRequester = appFocusRequester,
        emptyDetailFocusRequester = emptyDetailFocusRequester,
        telemetryContent = {
            FanTelemetrySection(
                state = FanTelemetrySectionState(
                    overlayEnabled = state.overlayEnabled,
                    overlayPermissionGranted = overlayPermissionGranted,
                    cpu = temperatureUi(thermal.cpuSummary),
                    gpu = temperatureUi(thermal.gpuSummary),
                    memoryTemperature = thermal.ddr?.let { formatTemperature(it.tempC) }
                        ?: unavailable,
                    batteryTemperature = thermal.battery?.let { formatTemperature(it.tempC) }
                        ?: unavailable,
                ),
                onOverlayClick = {
                    if (!overlayPermissionGranted) {
                        context.startActivity(
                            Intent(context, OverlayPermissionActivity::class.java)
                        )
                    } else {
                        vm.setOverlayEnabled(!state.overlayEnabled)
                    }
                },
                onOverlayEnabledChange = { checked ->
                    if (checked && !overlayPermissionGranted) {
                        context.startActivity(
                            Intent(context, OverlayPermissionActivity::class.java)
                        )
                    } else {
                        vm.setOverlayEnabled(checked)
                    }
                },
                onCpuClick = { detailKind = ThermalKind.CPU },
                onGpuClick = { detailKind = ThermalKind.GPU },
                overlayModifier = Modifier
                    .focusRequester(overlayFocusRequester)
                    .focusProperties {
                        up = FocusRequester.Default
                        down = telemetryFocusRequester
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
                telemetryPanelModifier = Modifier
                    .focusRequester(telemetryFocusRequester)
                    .focusProperties {
                        up = overlayFocusRequester
                        down = FocusRequester.Default
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
            )
        },
        fanContent = {
            FanProfilesSection(
                state = FanProfileSectionState(
                    profiles = state.fanConfig.catalog.visibleProfiles.map { profile ->
                        FanProfileItemUiState(
                            id = profile.id,
                            name = profile.displayName(context),
                            controlPointCount = profile.points.size,
                        )
                    },
                ),
                onProfileSelected = { profileId ->
                    editingProfileId = profileId
                },
                onDeleteProfile = vm::deleteFanCurve,
                showTitle = false,
                offModifier = Modifier
                    .focusRequester(fanProfileFocusRequesters[0])
                    .focusProperties {
                        up = FocusRequester.Default
                        down = if (fanProfileIds.isEmpty()) {
                            addCurveFocusRequester
                        } else {
                            fanProfileFocusRequesters[1]
                        }
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
                profileModifier = { index ->
                    val focusIndex = index + 1
                    Modifier
                        .focusRequester(fanProfileFocusRequesters[focusIndex])
                        .focusProperties {
                            up = fanProfileFocusRequesters[focusIndex - 1]
                            down = if (index == fanProfileIds.lastIndex) {
                                addCurveFocusRequester
                            } else {
                                fanProfileFocusRequesters[focusIndex + 1]
                            }
                            left = FocusRequester.Default
                            right = FocusRequester.Default
                        }
                },
            )
        },
        fanAction = {
            FanProfilesAddCurveButton(
                onClick = {
                    editingProfileId = vm.addFanCurve(
                        context.getString(R.string.new_fan_curve)
                    )
                },
                modifier = Modifier
                    .focusRequester(addCurveFocusRequester)
                    .focusProperties {
                        up = fanProfileFocusRequesters.last()
                        down = FocusRequester.Default
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
            )
        },
        joystickContent = {
            JoystickProfilesSection(
                profiles = state.joystickProfiles.visibleProfiles.map { profile ->
                    JoystickProfileUiState(
                        id = profile.id,
                        name = profile.name,
                        mode = profile.mode,
                        red = profile.red,
                        green = profile.green,
                        blue = profile.blue,
                        brightness = profile.brightness,
                    )
                },
                onProfileSelected = { editingJoystickProfileId = it },
                onDeleteProfile = vm::deleteJoystickProfile,
                offModifier = Modifier
                    .focusRequester(joystickProfileFocusRequesters[0])
                    .focusProperties {
                        up = FocusRequester.Default
                        down = if (joystickProfileIds.isEmpty()) {
                            addJoystickProfileFocusRequester
                        } else {
                            joystickProfileFocusRequesters[1]
                        }
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
                profileModifier = { index ->
                    val focusIndex = index + 1
                    Modifier
                        .focusRequester(joystickProfileFocusRequesters[focusIndex])
                        .focusProperties {
                            up = joystickProfileFocusRequesters[focusIndex - 1]
                            down = if (index == joystickProfileIds.lastIndex) {
                                addJoystickProfileFocusRequester
                            } else {
                                joystickProfileFocusRequesters[focusIndex + 1]
                            }
                            left = FocusRequester.Default
                            right = FocusRequester.Default
                        }
                },
            )
        },
        joystickAction = {
            AddJoystickProfileButton(
                onClick = {
                    editingJoystickProfileId = vm.addJoystickProfile(
                        context.getString(JoystickR.string.joystick_new_profile)
                    )
                },
                modifier = Modifier
                    .focusRequester(addJoystickProfileFocusRequester)
                    .focusProperties {
                        up = joystickProfileFocusRequesters.last()
                        down = FocusRequester.Default
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
            )
        },
        presetContent = {
            PresetManagementSection(
                presets = presetItems,
                onPresetClick = { editingPresetId = it },
                onDeletePreset = vm::deletePreset,
                itemModifier = { index ->
                    Modifier
                        .focusRequester(presetFocusRequesters[index])
                        .focusProperties {
                            up = if (index == 0) {
                                FocusRequester.Default
                            } else {
                                presetFocusRequesters[index - 1]
                            }
                            down = if (index == presetItems.lastIndex) {
                                addPresetFocusRequester
                            } else {
                                presetFocusRequesters[index + 1]
                            }
                            left = FocusRequester.Default
                            right = FocusRequester.Default
                        }
                },
            )
        },
        presetAction = {
            AddPresetButton(
                onClick = {
                    editingPresetId = vm.addPreset(context.getString(R.string.new_preset))
                },
                modifier = Modifier
                    .focusRequester(addPresetFocusRequester)
                    .focusProperties {
                        up = presetFocusRequesters.last()
                        down = FocusRequester.Default
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
            )
        },
        performanceContent = {
            PerformanceProfilesSection(
                config = state.performanceProfiles,
                onProfileSelected = { editingPerformanceProfileId = it },
                onDeleteProfile = vm::deletePerformanceProfile,
                profileModifier = { index ->
                    Modifier
                        .focusRequester(performanceProfileFocusRequesters[index])
                        .focusProperties {
                            up = if (index == 0) {
                                FocusRequester.Default
                            } else {
                                performanceProfileFocusRequesters[index - 1]
                            }
                            down = if (index == performanceProfileIds.lastIndex) {
                                addPerformanceProfileFocusRequester
                            } else {
                                performanceProfileFocusRequesters[index + 1]
                            }
                            left = FocusRequester.Default
                            right = FocusRequester.Default
                        }
                },
            )
        },
        performanceAction = {
            AddPerformanceProfileButton(
                onClick = {
                    vm.addPerformanceProfile(
                        context.getString(R.string.new_performance_profile)
                    )?.let { editingPerformanceProfileId = it }
                },
                modifier = Modifier
                    .focusRequester(addPerformanceProfileFocusRequester)
                    .focusProperties {
                        up = performanceProfileFocusRequesters.lastOrNull()
                            ?: FocusRequester.Default
                        down = FocusRequester.Default
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
            )
        },
        globalPresetContent = {
            val gameProfile = state.presetConfig.defaultPreset(true)
            val nonGameProfile = state.presetConfig.defaultPreset(false)
            DefaultProfileSection(
                gameProfileId = gameProfile.id,
                gameProfileName = gameProfile.name,
                nonGameProfileId = nonGameProfile.id,
                nonGameProfileName = nonGameProfile.name,
                profileChoices = state.presetConfig.catalog.presets.map {
                    AppProfileChoice(it.id, it.name)
                },
                fanCurveChoices = state.fanConfig.catalog.visibleProfiles.map {
                    AppProfileChoice(it.id, it.displayName(context))
                },
                joystickChoices = state.joystickProfiles.visibleProfiles.map {
                    AppProfileChoice(it.id, it.name)
                },
                performanceChoices = state.performanceProfiles.profiles.map {
                    AppProfileChoice(it.id, it.displayName(context))
                },
                onDefaultProfileSelected = { isGame, id ->
                    vm.selectDefaultProfile(isGame, id)
                    onFanCurveSelected(true)
                },
                onProfileEdit = { editingPresetId = it },
                onAddProfile = {
                    editingPresetId = vm.addPreset(context.getString(R.string.new_preset))
                },
                onFanCurveEdit = { editingProfileId = it },
                onAddFanCurve = {
                    editingProfileId = vm.addFanCurve(context.getString(R.string.new_fan_curve))
                },
                onJoystickEdit = { editingJoystickProfileId = it },
                onAddJoystick = {
                    editingJoystickProfileId = vm.addJoystickProfile(
                        context.getString(JoystickR.string.joystick_new_profile)
                    )
                },
                onPerformanceEdit = { editingPerformanceProfileId = it },
                onAddPerformance = {
                    vm.addPerformanceProfile(
                        context.getString(R.string.new_performance_profile)
                    )?.let { editingPerformanceProfileId = it }
                },
            )
        },
        appProfileContent = { packageName ->
            val profile = state.appProfiles[packageName]
            val app = state.installedApps.firstOrNull { it.packageName == packageName }
            val effectivePreset = AppProfilePreferences.effectivePreset(
                profile = profile,
                presetConfig = state.presetConfig,
                isGame = app?.isGame == true,
            )
            AppProfileSection(
                profile = profile,
                selectedProfileName = if (profile?.presetId == null) {
                    context.getString(R.string.follow_default)
                } else effectivePreset.name,
                selectedFanCurveName = profile?.fanCurveId?.let(::fanCurveName)
                    ?: context.getString(R.string.follow_profile),
                selectedJoystickProfileName = profile?.joystickId
                    ?.let(::joystickProfileName)
                    ?: context.getString(R.string.follow_profile),
                selectedPerformanceProfileName = profile?.performanceProfileId
                    ?.let(::performanceProfileName)
                    ?: context.getString(R.string.follow_profile),
                profileChoices = buildList {
                    add(AppProfileChoice(null, context.getString(R.string.follow_default)))
                    state.presetConfig.catalog.presets.forEach {
                        add(AppProfileChoice(it.id, it.name))
                    }
                },
                fanCurveChoices = buildList {
                    add(AppProfileChoice(null, context.getString(R.string.follow_profile)))
                    state.fanConfig.catalog.visibleProfiles.forEach { fanProfile ->
                        add(AppProfileChoice(fanProfile.id, fanProfile.displayName(context)))
                    }
                },
                joystickChoices = buildList {
                    add(AppProfileChoice(null, context.getString(R.string.follow_profile)))
                    state.joystickProfiles.visibleProfiles.forEach { joystickProfile ->
                        add(AppProfileChoice(joystickProfile.id, joystickProfile.name))
                    }
                },
                performanceChoices = buildList {
                    add(AppProfileChoice(null, context.getString(R.string.follow_profile)))
                    state.performanceProfiles.profiles.forEach { performanceProfile ->
                        add(
                            AppProfileChoice(
                                performanceProfile.id,
                                performanceProfile.displayName(context),
                            )
                        )
                    }
                },
                onProfileSelected = { vm.setAppPreset(packageName, it) },
                onFanCurveSelected = {
                    vm.setAppFanCurve(packageName, it)
                    if (it != null) onFanCurveSelected(true)
                },
                onJoystickSelected = { vm.setAppJoystickProfile(packageName, it) },
                onPerformanceSelected = { vm.setAppPerformanceProfile(packageName, it) },
                onProfileEdit = { editingPresetId = it },
                onAddProfile = {
                    editingPresetId = vm.addPreset(context.getString(R.string.new_preset))
                },
                onFanCurveEdit = { editingProfileId = it },
                onAddFanCurve = {
                    editingProfileId = vm.addFanCurve(context.getString(R.string.new_fan_curve))
                },
                onJoystickEdit = { editingJoystickProfileId = it },
                onAddJoystick = {
                    editingJoystickProfileId = vm.addJoystickProfile(
                        context.getString(JoystickR.string.joystick_new_profile)
                    )
                },
                onPerformanceEdit = { editingPerformanceProfileId = it },
                onAddPerformance = {
                    vm.addPerformanceProfile(
                        context.getString(R.string.new_performance_profile)
                    )?.let { editingPerformanceProfileId = it }
                },
            )
        },
        accessContent = {
            AuthorizationManagementSection(
                state = AuthorizationUiState(
                    autoStartEnabled = state.autoStartEnabled,
                    rootGranted = hasRoot,
                    overlayPermissionGranted = overlayPermissionGranted,
                    notificationsEnabled = notificationsEnabled,
                    microphoneGranted = microphoneGranted,
                ),
                onAutoStartEnabledChange = vm::setAutoStartEnabled,
                onRefreshRoot = onRefreshRoot,
                onOpenKernelSu = { context.openKernelSu() },
                onOpenAppInfo = { context.openAppInfo() },
                onOpenOverlaySettings = { context.openOverlaySettings() },
                onOpenNotificationSettings = { context.openFanNotificationSettings() },
                onRequestMicrophone = {
                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onRequestScreenCapture = {
                    context.startActivity(MediaProjectionActivity.createIntent(context))
                },
                autoStartModifier = Modifier
                    .focusRequester(authorizationFocusRequesters[0])
                    .focusProperties {
                        up = FocusRequester.Default
                        down = authorizationFocusRequesters[1]
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
                rootModifier = Modifier
                    .focusRequester(authorizationFocusRequesters[1])
                    .focusProperties {
                        up = authorizationFocusRequesters[0]
                        down = authorizationFocusRequesters[2]
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
                kernelSuModifier = Modifier
                    .focusRequester(authorizationFocusRequesters[2])
                    .focusProperties {
                        up = authorizationFocusRequesters[1]
                        down = authorizationFocusRequesters[3]
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
                appInfoModifier = Modifier
                    .focusRequester(authorizationFocusRequesters[3])
                    .focusProperties {
                        up = authorizationFocusRequesters[2]
                        down = authorizationFocusRequesters[4]
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
                overlayModifier = Modifier
                    .focusRequester(authorizationFocusRequesters[4])
                    .focusProperties {
                        up = authorizationFocusRequesters[3]
                        down = authorizationFocusRequesters[5]
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
                notificationsModifier = Modifier
                    .focusRequester(authorizationFocusRequesters[5])
                    .focusProperties {
                        up = authorizationFocusRequesters[4]
                        down = authorizationFocusRequesters[6]
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
                microphoneModifier = Modifier
                    .focusRequester(authorizationFocusRequesters[6])
                    .focusProperties {
                        up = authorizationFocusRequesters[5]
                        down = authorizationFocusRequesters[7]
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
                screenCaptureModifier = Modifier
                    .focusRequester(authorizationFocusRequesters[7])
                    .focusProperties {
                        up = authorizationFocusRequesters[6]
                        down = githubFocusRequester
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
            )
        },
        footer = {
            AppFooter(
                linkModifier = Modifier
                    .focusRequester(githubFocusRequester)
                    .focusProperties {
                        up = authorizationFocusRequesters[7]
                        down = FocusRequester.Default
                        left = FocusRequester.Default
                        right = FocusRequester.Default
                    },
            )
        },
    )

    editingProfileId?.let { profileId ->
        val profile = state.fanConfig.catalog.profile(profileId) ?: return@let
        FanCurveEditorDialog(
            profileId = profile.id,
            profileName = profile.displayName(context),
            points = profile.points,
            defaultPoints = profile.defaultPoints,
            currentTempC = thermal.controlTempC,
            onPointsChanged = { vm.setFanCurve(profile.id, it) },
            onSetDefault = { vm.setFanCurveAsDefault(profile.id, it) },
            onReset = { vm.resetFanCurve(profile.id) },
            onRename = { vm.renameFanCurve(profile.id, it) },
            onDelete = {
                vm.deleteFanCurve(profile.id)
                restoreFanCurveFocusId = profile.id
                editingProfileId = null
            },
            onDismiss = {
                restoreFanCurveFocusId = profile.id
                editingProfileId = null
            },
        )
    }

    editingPresetId?.let { presetId ->
        val preset = state.presetConfig.catalog.preset(presetId) ?: return@let
        PresetEditorDialog(
            preset = preset,
            fanCurveName = fanCurveName(preset.fanCurveId),
            fanCurveChoices = buildList {
                add(PresetFanCurveChoice(id = null, name = offName))
                state.fanConfig.catalog.visibleProfiles.forEach { profile ->
                    add(
                        PresetFanCurveChoice(
                            id = profile.id,
                            name = profile.displayName(context),
                        )
                    )
                }
            },
            onFanCurveSelected = {
                vm.setPresetFanCurve(preset.id, it)
                if (it != null) onFanCurveSelected(true)
            },
            onFanCurveEdit = { editingProfileId = it },
            onAddFanCurve = {
                editingProfileId = vm.addFanCurve(context.getString(R.string.new_fan_curve))
            },
            joystickProfileName = joystickProfileName(preset.joystickId),
            joystickChoices = buildList {
                add(PresetJoystickChoice(id = null, name = joystickOffName))
                state.joystickProfiles.visibleProfiles.forEach { profile ->
                    add(PresetJoystickChoice(id = profile.id, name = profile.name))
                }
            },
            onJoystickSelected = { vm.setPresetJoystickProfile(preset.id, it) },
            onJoystickEdit = { editingJoystickProfileId = it },
            onAddJoystick = {
                editingJoystickProfileId = vm.addJoystickProfile(
                    context.getString(JoystickR.string.joystick_new_profile)
                )
            },
            performanceProfileName = performanceProfileName(preset.performanceProfileId),
            performanceChoices = buildList {
                add(
                    PresetPerformanceChoice(
                        id = null,
                        name = unmanagedPerformanceName,
                    )
                )
                state.performanceProfiles.profiles.forEach { profile ->
                    add(
                        PresetPerformanceChoice(
                            id = profile.id,
                            name = profile.displayName(context),
                        )
                    )
                }
            },
            onPerformanceSelected = {
                vm.setPresetPerformanceProfile(preset.id, it)
            },
            onPerformanceEdit = { editingPerformanceProfileId = it },
            onAddPerformance = {
                vm.addPerformanceProfile(
                    context.getString(R.string.new_performance_profile)
                )?.let { editingPerformanceProfileId = it }
            },
            onRename = { vm.renamePreset(preset.id, it) },
            onDelete = {
                vm.deletePreset(preset.id)
                restorePresetFocusId = preset.id
                editingPresetId = null
            },
            onDismiss = {
                restorePresetFocusId = preset.id
                editingPresetId = null
            },
        )
    }

    editingPerformanceProfileId?.let { profileId ->
        val profile = state.performanceProfiles.profile(profileId) ?: return@let
        PerformanceProfileEditorDialog(
            profile = profile,
            policies = state.performanceProfiles.policies,
            onSave = { name, frequencies ->
                vm.updatePerformanceProfile(profile.id, name, frequencies)
                restorePerformanceFocusId = profile.id
                editingPerformanceProfileId = null
            },
            onDelete = {
                vm.deletePerformanceProfile(profile.id)
                restorePerformanceFocusId = profile.id
                editingPerformanceProfileId = null
            },
            onDismiss = {
                restorePerformanceFocusId = profile.id
                editingPerformanceProfileId = null
            },
        )
    }

    editingJoystickProfileId?.let { profileId ->
        val profile = joystickProfileUi(profileId) ?: return@let
        JoystickProfileEditorDialog(
            profile = profile,
            onModeSelected = { mode ->
                vm.setJoystickMode(profile.id, mode)
                if (mode == JoystickRgbMode.AMBILIGHT) {
                    context.startActivity(MediaProjectionActivity.createIntent(context))
                }
            },
            onColorSelected = { red, green, blue ->
                vm.setJoystickColor(profile.id, red, green, blue)
            },
            onBrightnessSelected = { vm.setJoystickBrightness(profile.id, it) },
            onRename = { vm.renameJoystickProfile(profile.id, it) },
            onDelete = {
                vm.deleteJoystickProfile(profile.id)
                restoreJoystickFocusId = profile.id
                editingJoystickProfileId = null
            },
            onDismiss = {
                restoreJoystickFocusId = profile.id
                editingJoystickProfileId = null
            },
        )
    }

    detailKind?.let { kind ->
        val readings = when (kind) {
            ThermalKind.CPU -> thermal.cpu
            ThermalKind.GPU -> thermal.gpu
            else -> emptyList()
        }
        ThermalDetailsDialog(
            title = if (kind == ThermalKind.CPU) {
                stringResource(R.string.cpu)
            } else {
                stringResource(R.string.gpu)
            },
            readings = readings,
            onDismiss = { detailKind = null },
        )
    }
}

@Composable
private fun ThermalDetailsDialog(
    title: String,
    readings: List<ThermalReading>,
    onDismiss: () -> Unit,
) {
    val summary = remember(readings) {
        if (readings.isEmpty()) {
            TemperatureSummary()
        } else {
            val hottest = readings.maxBy { it.tempC }
            TemperatureSummary(
                averageC = readings.map { it.tempC }.average(),
                maxC = hottest.tempC,
                count = readings.size,
                hottest = hottest,
            )
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.thermal_details_title, title)) },
        text = {
            if (readings.isEmpty()) {
                Text(stringResource(R.string.no_thermal_readings))
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailRow(
                        stringResource(R.string.thermal_average),
                        formatTemperature(summary.averageC),
                    )
                    DetailRow(
                        stringResource(R.string.thermal_hottest),
                        "${summary.hottest?.name}  ${formatTemperature(summary.maxC)}",
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.all_hotspots),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    readings.forEach { reading ->
                        DetailRow(reading.name, formatTemperature(reading.tempC))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FanCurveEditorDialog(
    profileId: String,
    profileName: String,
    points: List<FanCurvePoint>,
    defaultPoints: List<FanCurvePoint>,
    currentTempC: Double,
    onPointsChanged: (List<FanCurvePoint>) -> Unit,
    onSetDefault: (List<FanCurvePoint>) -> Unit,
    onReset: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val draft = remember(profileId) {
        mutableStateListOf<FanCurvePoint>().apply { addAll(points.sortedBy { it.tempC }) }
    }
    var selectedIndex by remember(profileId) { mutableIntStateOf(-1) }
    var showRenameDialog by remember(profileId) { mutableStateOf(false) }
    var renameDraft by remember(profileId) { mutableStateOf(profileName) }
    var showDeleteDialog by remember(profileId) { mutableStateOf(false) }
    LaunchedEffect(profileName, showRenameDialog) {
        if (!showRenameDialog) renameDraft = profileName
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data?.takeIf { result.resultCode == Activity.RESULT_OK }
        if (uri != null) {
            val exported = runCatching {
                val json = FanCurveJson.encode(
                    profileId = profileId,
                    profileName = profileName,
                    points = draft.toList(),
                )
                context.contentResolver.openOutputStream(uri, "wt")
                    ?.bufferedWriter()
                    ?.use { writer -> writer.write(json) }
                    ?: error("Unable to open export destination")
            }.isSuccess
            Toast.makeText(
                context,
                if (exported) R.string.curve_exported else R.string.curve_export_failed,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val imported = runCatching {
                val displayName = context.contentResolver.displayName(uri)
                require(displayName == null || displayName.endsWith(".json", ignoreCase = true))
                val json = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { reader -> reader.readText() }
                    ?: error("Unable to open selected file")
                FanCurveJson.decode(json)
            }.getOrNull()
            if (imported != null) {
                draft.clear()
                draft.addAll(imported)
                selectedIndex = -1
                onPointsChanged(imported)
            }
            Toast.makeText(
                context,
                if (imported != null) R.string.curve_imported else R.string.curve_import_failed,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profileName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit_square),
                            contentDescription = stringResource(R.string.rename_curve),
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = stringResource(R.string.delete_curve),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                BoxWithConstraints(Modifier.weight(1f)) {
                    val compact = maxWidth < 700.dp
                    val graph: @Composable (Modifier) -> Unit = { modifier ->
                        CurveGraphPanel(
                            modifier = modifier,
                            points = draft,
                            selectedIndex = selectedIndex,
                            currentTempC = currentTempC,
                            onSelectedIndexChanged = { selectedIndex = it },
                            onCommit = { onPointsChanged(draft.toList()) },
                            onSetDefault = {
                                onSetDefault(draft.toList())
                            },
                            onReset = {
                                draft.clear()
                                draft.addAll(defaultPoints.sortedBy { it.tempC })
                                selectedIndex = -1
                                onReset()
                            },
                            onImport = {
                                importLauncher.launch(arrayOf("application/json"))
                            },
                            onExport = {
                                exportLauncher.launch(fanCurveExportIntent())
                            },
                        )
                    }
                    val controls: @Composable (Modifier) -> Unit = { modifier ->
                        ControlPointList(
                            modifier = modifier,
                            points = draft,
                            selectedIndex = selectedIndex,
                            onSelectedIndexChanged = { selectedIndex = it },
                            onCommit = { onPointsChanged(draft.toList()) },
                        )
                    }

                    if (compact) {
                        Column(Modifier.fillMaxSize()) {
                            graph(
                                Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                            )
                            Spacer(Modifier.height(14.dp))
                            controls(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    } else {
                        Row(Modifier.fillMaxSize()) {
                            graph(
                                Modifier
                                    .weight(1.25f)
                                    .fillMaxHeight()
                            )
                            Spacer(Modifier.width(22.dp))
                            controls(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.rename_curve)) },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.curve_name)) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameDraft.isNotBlank()) {
                            onRename(renameDraft)
                            showRenameDialog = false
                        }
                    },
                    enabled = renameDraft.isNotBlank(),
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_curve)) },
            text = { Text(stringResource(R.string.delete_curve_confirmation, profileName)) },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun CurveGraphPanel(
    modifier: Modifier,
    points: MutableList<FanCurvePoint>,
    selectedIndex: Int,
    currentTempC: Double,
    onSelectedIndexChanged: (Int) -> Unit,
    onCommit: () -> Unit,
    onSetDefault: () -> Unit,
    onReset: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    Column(modifier) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CurveFileButton(
                onClick = onSetDefault,
                icon = { Icon(Icons.Default.BookmarkAdd, null, Modifier.size(17.dp)) },
                label = stringResource(R.string.set_as_default),
            )
            CurveFileButton(
                onClick = onReset,
                icon = { Icon(Icons.Default.Restore, null, Modifier.size(17.dp)) },
                label = stringResource(R.string.reset),
            )
            CurveFileButton(
                onClick = onImport,
                icon = { Icon(Icons.Default.FileOpen, null, Modifier.size(17.dp)) },
                label = stringResource(R.string.import_curve),
            )
            CurveFileButton(
                onClick = onExport,
                icon = { Icon(Icons.Default.SaveAlt, null, Modifier.size(17.dp)) },
                label = stringResource(R.string.export_curve),
            )
        }
        Spacer(Modifier.height(10.dp))
        CurveGraph(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            points = points,
            selectedIndex = selectedIndex,
            currentTempC = currentTempC,
            onSelectedIndexChanged = onSelectedIndexChanged,
            onCommit = onCommit,
        )
    }
}

@Composable
private fun CurveFileButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp),
    ) {
        icon()
        Spacer(Modifier.width(7.dp))
        Text(label)
    }
}

private fun android.content.ContentResolver.displayName(uri: Uri): String? =
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }

private fun fanCurveExportIntent(): Intent =
    Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/json"
        putExtra(Intent.EXTRA_TITLE, "fan curve.json")
        putExtra(
            DocumentsContract.EXTRA_INITIAL_URI,
            DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                "primary:Download",
            ),
        )
    }

private fun Context.areFanNotificationsEnabled(): Boolean {
    if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
    val channel = getSystemService(NotificationManager::class.java)
        .getNotificationChannel(SystemControlService.CHANNEL_ID)
    return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
}

private fun Context.openFanNotificationSettings() {
    val channelSettings = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        putExtra(Settings.EXTRA_CHANNEL_ID, SystemControlService.CHANNEL_ID)
    }
    val appSettings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }
    startActivity(
        if (channelSettings.resolveActivity(packageManager) != null) {
            channelSettings
        } else {
            appSettings
        }
    )
}

private fun Context.openAppInfo() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
    )
}

private fun Context.openOverlaySettings() {
    val appOverlaySettings = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.fromParts("package", packageName, null),
    )
    val generalOverlaySettings = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
    startActivity(
        if (appOverlaySettings.resolveActivity(packageManager) != null) {
            appOverlaySettings
        } else {
            generalOverlaySettings
        }
    )
}

private fun Context.openKernelSu() {
    val managerIntent = listOf(
        "me.weishu.kernelsu",
        "com.rifsxd.ksunext",
    ).firstNotNullOfOrNull { packageName ->
        packageManager.getLaunchIntentForPackage(packageName)
    }
    if (managerIntent != null) {
        startActivity(managerIntent)
    } else {
        Toast.makeText(this, R.string.kernelsu_not_found, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun AppFooter(
    modifier: Modifier = Modifier,
    linkModifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val githubUrl = stringResource(R.string.github_url)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "RetroControl v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = linkModifier
                .clip(RoundedCornerShape(8.dp))
                .bringIntoViewOnFocus()
                .clickable {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                    )
                }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_github),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.github),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = stringResource(R.string.open_github),
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CurveGraph(
    modifier: Modifier,
    points: MutableList<FanCurvePoint>,
    selectedIndex: Int,
    currentTempC: Double,
    onSelectedIndexChanged: (Int) -> Unit,
    onCommit: () -> Unit,
) {
    var graphSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val hitRadius = with(density) { 24.dp.toPx() }
    val padLeft = with(density) { 45.dp.toPx() }
    val padRight = with(density) { 18.dp.toPx() }
    val padTop = with(density) { 18.dp.toPx() }
    val padBottom = with(density) { 34.dp.toPx() }

    fun graphWidth(size: IntSize) = (size.width - padLeft - padRight).coerceAtLeast(1f)
    fun graphHeight(size: IntSize) = (size.height - padTop - padBottom).coerceAtLeast(1f)
    fun pointOffset(point: FanCurvePoint, size: IntSize): Offset = Offset(
        x = padLeft + (point.tempC - 20f) / 80f * graphWidth(size),
        y = padTop + graphHeight(size) -
            point.speedPercent / 100f * graphHeight(size),
    )
    fun positionToPoint(position: Offset, index: Int, size: IntSize): FanCurvePoint {
        val rawTemp = (20f + ((position.x - padLeft) / graphWidth(size)) * 80f).roundToInt()
        val minTemp = if (index > 0) points[index - 1].tempC + 1 else 20
        val maxTemp = if (index < points.lastIndex) points[index + 1].tempC - 1 else 100
        return FanCurvePoint(
            tempC = rawTemp.coerceIn(minTemp, maxTemp),
            speedPercent = (
                ((padTop + graphHeight(size) - position.y) / graphHeight(size) * 100f) /
                    5f
                )
                .roundToInt()
                .times(5)
                .coerceIn(0, 100),
        )
    }
    fun nearestPoint(position: Offset, size: IntSize): Int =
        points.indices.minByOrNull { index ->
            val offset = pointOffset(points[index], size)
            hypot((offset.x - position.x).toDouble(), (offset.y - position.y).toDouble())
        }?.takeIf { index ->
            val offset = pointOffset(points[index], size)
            hypot((offset.x - position.x).toDouble(), (offset.y - position.y).toDouble()) <=
                hitRadius
        } ?: -1
    fun addPoint(position: Offset, size: IntSize) {
        if (
            position.x !in padLeft..(size.width - padRight) ||
            position.y !in padTop..(size.height - padBottom)
        ) return
        val temp = (20f + ((position.x - padLeft) / graphWidth(size)) * 80f)
            .roundToInt()
            .coerceIn(20, 100)
        if (points.any { it.tempC == temp }) return
        val speedPercent = (
            ((padTop + graphHeight(size) - position.y) / graphHeight(size) * 100f) / 5f
            )
            .roundToInt()
            .times(5)
            .coerceIn(0, 100)
        val updated = (points + FanCurvePoint(temp, speedPercent)).sortedBy { it.tempC }
        points.clear()
        points.addAll(updated)
        onSelectedIndexChanged(points.indexOfFirst { it.tempC == temp })
        onCommit()
    }

    val displayedPoints = points.toList()
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    val error = MaterialTheme.colorScheme.error

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .onSizeChanged { graphSize = it }
            .pointerInput(graphSize, points.size) {
                if (graphSize == IntSize.Zero) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val size = graphSize
                    val hit = nearestPoint(down.position, size)
                    val dragStart = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                        change.consume()
                    }
                    if (dragStart == null) {
                        if (hit >= 0) {
                            onSelectedIndexChanged(hit)
                        } else {
                            addPoint(down.position, size)
                        }
                    } else if (hit >= 0 && hit in points.indices) {
                        onSelectedIndexChanged(hit)
                        points[hit] = positionToPoint(dragStart.position, hit, size)
                        drag(dragStart.id) { change ->
                            if (hit in points.indices) {
                                points[hit] = positionToPoint(change.position, hit, size)
                                change.consume()
                            }
                        }
                        onCommit()
                    }
                }
            }
    ) {
        val graphW = size.width - padLeft - padRight
        val graphH = size.height - padTop - padBottom
        fun tempToX(temp: Float) = padLeft + (temp - 20f) / 80f * graphW
        fun percentToY(percent: Float) = padTop + graphH - percent / 100f * graphH

        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(170, 210, 210, 210)
            textSize = 20f
            isAntiAlias = true
        }
        for (temp in 20..100 step 10) {
            val x = tempToX(temp.toFloat())
            drawLine(
                onSurface.copy(alpha = 0.09f),
                Offset(x, padTop),
                Offset(x, size.height - padBottom),
            )
            drawContext.canvas.nativeCanvas.drawText(
                "$temp°",
                x - 12f,
                size.height - 7f,
                labelPaint,
            )
        }
        for (percent in 0..100 step 25) {
            val y = percentToY(percent.toFloat())
            drawLine(
                onSurface.copy(alpha = 0.09f),
                Offset(padLeft, y),
                Offset(size.width - padRight, y),
            )
            drawContext.canvas.nativeCanvas.drawText(
                "$percent%",
                3f,
                y + 6f,
                labelPaint,
            )
        }

        if (displayedPoints.isNotEmpty()) {
            val first = displayedPoints.first()
            val last = displayedPoints.last()
            val dashedPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            val extensionColor = primary.copy(alpha = 0.48f)
            if (first.tempC > 20) {
                val firstX = tempToX(first.tempC.toFloat())
                val zeroY = percentToY(0f)
                drawLine(
                    extensionColor,
                    Offset(padLeft, zeroY),
                    Offset(firstX, zeroY),
                    strokeWidth = 2.5f,
                    pathEffect = dashedPathEffect,
                )
                drawLine(
                    extensionColor,
                    Offset(firstX, zeroY),
                    Offset(firstX, percentToY(first.speedPercent.toFloat())),
                    strokeWidth = 2.5f,
                    pathEffect = dashedPathEffect,
                )
            }
            if (last.tempC < 100) {
                val lastY = percentToY(last.speedPercent.toFloat())
                drawLine(
                    extensionColor,
                    Offset(tempToX(last.tempC.toFloat()), lastY),
                    Offset(size.width - padRight, lastY),
                    strokeWidth = 2.5f,
                    pathEffect = dashedPathEffect,
                )
            }
        }

        if (displayedPoints.size >= 2) {
            val path = Path()
            displayedPoints.forEachIndexed { index, point ->
                val offset = Offset(
                    tempToX(point.tempC.toFloat()),
                    percentToY(point.speedPercent.toFloat()),
                )
                if (index == 0) path.moveTo(offset.x, offset.y)
                else path.lineTo(offset.x, offset.y)
            }
            drawPath(path, primary, style = Stroke(5f, cap = StrokeCap.Round))
        }
        displayedPoints.forEachIndexed { index, point ->
            val center = Offset(
                tempToX(point.tempC.toFloat()),
                percentToY(point.speedPercent.toFloat()),
            )
            val selected = index == selectedIndex
            drawCircle(primary.copy(alpha = 0.24f), if (selected) 22f else 17f, center)
            drawCircle(primary, if (selected) 13f else 10f, center)
            drawCircle(Color.White, 4.5f, center)
        }
        if (currentTempC in 20.0..100.0) {
            val x = tempToX(currentTempC.toFloat())
            drawLine(
                error.copy(alpha = 0.75f),
                Offset(x, padTop),
                Offset(x, size.height - padBottom),
                strokeWidth = 2f,
            )
        }
    }
}

@Composable
private fun ControlPointList(
    modifier: Modifier,
    points: MutableList<FanCurvePoint>,
    selectedIndex: Int,
    onSelectedIndexChanged: (Int) -> Unit,
    onCommit: () -> Unit,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.control_points),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        points.forEachIndexed { index, point ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelectedIndexChanged(index) },
                shape = RoundedCornerShape(16.dp),
                color = if (index == selectedIndex) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(
                                R.string.control_point_value,
                                point.tempC,
                                formatFanPercent(point.speedPercent),
                            ),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        if (points.size > 2) {
                            IconButton(
                                onClick = {
                                    points.removeAt(index)
                                    onSelectedIndexChanged(-1)
                                    onCommit()
                                },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    }
                    Slider(
                        value = point.tempC.toFloat(),
                        onValueChange = { value ->
                            val min = if (index > 0) points[index - 1].tempC + 1 else 20
                            val max = if (index < points.lastIndex) {
                                points[index + 1].tempC - 1
                            } else {
                                100
                            }
                            points[index] = point.copy(
                                tempC = value.roundToInt().coerceIn(min, max)
                            )
                            onSelectedIndexChanged(index)
                        },
                        onValueChangeFinished = onCommit,
                        valueRange = 20f..100f,
                        modifier = Modifier.height(28.dp),
                    )
                    Slider(
                        value = point.speedPercent.toFloat(),
                        onValueChange = { value ->
                            points[index] = point.copy(
                                speedPercent = (value / 5f)
                                    .roundToInt()
                                    .times(5)
                                    .coerceIn(0, 100)
                            )
                            onSelectedIndexChanged(index)
                        },
                        onValueChangeFinished = onCommit,
                        valueRange = 0f..100f,
                        steps = 19,
                        modifier = Modifier.height(28.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = {
                val widestGap = (0 until points.lastIndex)
                    .maxByOrNull { points[it + 1].tempC - points[it].tempC }
                val temp = widestGap?.let {
                    (points[it].tempC + points[it + 1].tempC) / 2
                } ?: 50
                val speedPercent = widestGap?.let {
                    (points[it].speedPercent + points[it + 1].speedPercent) / 2
                } ?: 50
                if (points.none { it.tempC == temp }) {
                    val updated = (points + FanCurvePoint(temp, speedPercent))
                        .sortedBy { it.tempC }
                    points.clear()
                    points.addAll(updated)
                    onSelectedIndexChanged(points.indexOfFirst { it.tempC == temp })
                    onCommit()
                }
            },
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add_point))
        }
        Spacer(Modifier.height(8.dp))
    }
}
