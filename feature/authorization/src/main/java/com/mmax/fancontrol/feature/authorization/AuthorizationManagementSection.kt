package com.mmax.fancontrol.feature.authorization

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mmax.fancontrol.designsystem.SettingsPreferenceRow
import com.mmax.fancontrol.designsystem.SettingsSectionTitle
import com.mmax.fancontrol.designsystem.SettingsSegmentGroup
import com.mmax.fancontrol.designsystem.SettingsTokens

data class AuthorizationUiState(
    val autoStartEnabled: Boolean,
    val rootGranted: Boolean,
    val notificationsEnabled: Boolean,
)

/**
 * A host-independent authorization card. The host supplies platform actions,
 * which keeps KernelSU, app-info and notification routing reusable.
 */
@Composable
fun AuthorizationManagementSection(
    state: AuthorizationUiState,
    onAutoStartEnabledChange: (Boolean) -> Unit,
    onRefreshRoot: () -> Unit,
    onOpenKernelSu: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSectionTitle(
        text = stringResource(R.string.authorization_title),
        modifier = modifier.padding(bottom = SettingsTokens.sectionTitleBottomPadding),
    )
    SettingsSegmentGroup {
        SettingsPreferenceRow(
            index = 0,
            count = 5,
            title = stringResource(R.string.authorization_auto_start),
            summary = stringResource(
                if (state.autoStartEnabled) {
                    R.string.authorization_auto_start_enabled
                } else {
                    R.string.authorization_auto_start_disabled
                }
            ),
            onClick = { onAutoStartEnabledChange(!state.autoStartEnabled) },
            trailingContent = {
                Switch(
                    checked = state.autoStartEnabled,
                    onCheckedChange = onAutoStartEnabledChange,
                )
            },
        )
        SettingsPreferenceRow(
            index = 1,
            count = 5,
            title = stringResource(R.string.authorization_root),
            summary = stringResource(
                if (state.rootGranted) {
                    R.string.authorization_root_granted
                } else {
                    R.string.authorization_root_not_granted
                }
            ),
            onClick = onRefreshRoot,
            trailingIcon = if (state.rootGranted) {
                Icons.Default.Check
            } else {
                Icons.Default.Refresh
            },
        )
        SettingsPreferenceRow(
            index = 2,
            count = 5,
            title = stringResource(R.string.authorization_kernelsu),
            summary = stringResource(R.string.authorization_kernelsu_summary),
            onClick = onOpenKernelSu,
            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
        )
        SettingsPreferenceRow(
            index = 3,
            count = 5,
            title = stringResource(R.string.authorization_app_info),
            summary = stringResource(R.string.authorization_app_info_summary),
            onClick = onOpenAppInfo,
            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
        )
        SettingsPreferenceRow(
            index = 4,
            count = 5,
            title = stringResource(R.string.authorization_notifications),
            summary = stringResource(
                if (state.notificationsEnabled) {
                    R.string.authorization_notifications_enabled
                } else {
                    R.string.authorization_notifications_disabled
                }
            ),
            onClick = onOpenNotificationSettings,
            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
        )
    }
}
