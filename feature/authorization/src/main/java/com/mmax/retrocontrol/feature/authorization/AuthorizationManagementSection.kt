package com.mmax.retrocontrol.feature.authorization

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.stringResource
import com.mmax.retrocontrol.designsystem.SettingsPreferenceRow
import com.mmax.retrocontrol.designsystem.SettingsSectionTitle
import com.mmax.retrocontrol.designsystem.SettingsSegmentGroup
import com.mmax.retrocontrol.designsystem.SettingsTokens

data class AuthorizationUiState(
    val telemetryOverlayEnabled: Boolean,
    val autoStartEnabled: Boolean,
    val rootGranted: Boolean,
    val overlayPermissionGranted: Boolean,
    val notificationsEnabled: Boolean,
    val microphoneGranted: Boolean,
)

/**
 * A host-independent authorization card. The host supplies platform actions,
 * which keeps KernelSU, app-info and notification routing reusable.
 */
@Composable
fun AuthorizationManagementSection(
    state: AuthorizationUiState,
    onTelemetryOverlayClick: () -> Unit,
    onTelemetryOverlayEnabledChange: (Boolean) -> Unit,
    onAutoStartEnabledChange: (Boolean) -> Unit,
    onRefreshRoot: () -> Unit,
    onOpenKernelSu: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRequestMicrophone: () -> Unit,
    onRequestScreenCapture: () -> Unit,
    modifier: Modifier = Modifier,
    telemetryOverlayModifier: Modifier = Modifier,
    autoStartModifier: Modifier = Modifier,
    rootModifier: Modifier = Modifier,
    kernelSuModifier: Modifier = Modifier,
    appInfoModifier: Modifier = Modifier,
    overlayModifier: Modifier = Modifier,
    notificationsModifier: Modifier = Modifier,
    microphoneModifier: Modifier = Modifier,
    screenCaptureModifier: Modifier = Modifier,
) {
    SettingsSectionTitle(
        text = stringResource(R.string.authorization_title),
        modifier = modifier.padding(bottom = SettingsTokens.sectionTitleBottomPadding),
    )
    SettingsSegmentGroup {
        SettingsPreferenceRow(
            index = 0,
            count = 9,
            title = stringResource(R.string.authorization_telemetry_overlay),
            summary = stringResource(
                if (state.overlayPermissionGranted) {
                    R.string.authorization_telemetry_overlay_summary
                } else {
                    R.string.authorization_telemetry_overlay_permission_missing
                }
            ),
            onClick = onTelemetryOverlayClick,
            modifier = telemetryOverlayModifier,
            trailingContent = {
                Switch(
                    checked = state.telemetryOverlayEnabled && state.overlayPermissionGranted,
                    onCheckedChange = onTelemetryOverlayEnabledChange,
                    modifier = Modifier.focusProperties { canFocus = false },
                )
            },
        )
        SettingsPreferenceRow(
            index = 1,
            count = 9,
            title = stringResource(R.string.authorization_auto_start),
            onClick = { onAutoStartEnabledChange(!state.autoStartEnabled) },
            modifier = autoStartModifier,
            trailingContent = {
                Switch(
                    checked = state.autoStartEnabled,
                    onCheckedChange = onAutoStartEnabledChange,
                    modifier = Modifier.focusProperties { canFocus = false },
                )
            },
        )
        SettingsPreferenceRow(
            index = 2,
            count = 9,
            title = stringResource(R.string.authorization_root),
            summary = stringResource(
                if (state.rootGranted) {
                    R.string.authorization_root_granted
                } else {
                    R.string.authorization_root_not_granted
                }
            ),
            onClick = onRefreshRoot,
            modifier = rootModifier,
            trailingIcon = if (state.rootGranted) {
                Icons.Default.Check
            } else {
                Icons.Default.Refresh
            },
        )
        SettingsPreferenceRow(
            index = 3,
            count = 9,
            title = stringResource(R.string.authorization_kernelsu),
            summary = stringResource(R.string.authorization_kernelsu_summary),
            onClick = onOpenKernelSu,
            modifier = kernelSuModifier,
            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
        )
        SettingsPreferenceRow(
            index = 4,
            count = 9,
            title = stringResource(R.string.authorization_app_info),
            summary = stringResource(R.string.authorization_app_info_summary),
            onClick = onOpenAppInfo,
            modifier = appInfoModifier,
            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
        )
        SettingsPreferenceRow(
            index = 5,
            count = 9,
            title = stringResource(R.string.authorization_overlay),
            summary = stringResource(
                if (state.overlayPermissionGranted) {
                    R.string.authorization_overlay_granted
                } else {
                    R.string.authorization_overlay_not_granted
                }
            ),
            onClick = onOpenOverlaySettings,
            modifier = overlayModifier,
            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
        )
        SettingsPreferenceRow(
            index = 6,
            count = 9,
            title = stringResource(R.string.authorization_notifications),
            summary = stringResource(
                if (state.notificationsEnabled) {
                    R.string.authorization_notifications_enabled
                } else {
                    R.string.authorization_notifications_disabled
                }
            ),
            onClick = onOpenNotificationSettings,
            modifier = notificationsModifier,
            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
        )
        SettingsPreferenceRow(
            index = 7,
            count = 9,
            title = stringResource(R.string.authorization_microphone),
            summary = stringResource(
                if (state.microphoneGranted) {
                    R.string.authorization_microphone_granted
                } else {
                    R.string.authorization_microphone_not_granted
                }
            ),
            onClick = onRequestMicrophone,
            modifier = microphoneModifier,
            trailingIcon = if (state.microphoneGranted) {
                Icons.Default.Check
            } else {
                Icons.AutoMirrored.Filled.OpenInNew
            },
        )
        SettingsPreferenceRow(
            index = 8,
            count = 9,
            title = stringResource(R.string.authorization_screen_capture),
            summary = stringResource(R.string.authorization_screen_capture_summary),
            onClick = onRequestScreenCapture,
            modifier = screenCaptureModifier,
            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
        )
    }
}
