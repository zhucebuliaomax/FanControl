@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.mmax.fancontrol.feature.fan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mmax.fancontrol.designsystem.SettingsPreferenceRow
import com.mmax.fancontrol.designsystem.SettingsSectionTitle
import com.mmax.fancontrol.designsystem.SettingsSegmentGroup
import com.mmax.fancontrol.designsystem.SettingsTokens

data class FanProfileSectionState(
    val enabled: Boolean,
    val activeCurveName: String = "",
    val controlPointCount: Int = 0,
)

data class TemperatureTileUiState(
    val average: String,
    val hottest: String,
)

data class FanTelemetrySectionState(
    val overlayEnabled: Boolean,
    val overlayPermissionGranted: Boolean,
    val cpu: TemperatureTileUiState,
    val gpu: TemperatureTileUiState,
    val memoryTemperature: String,
    val batteryTemperature: String,
)

/**
 * Host-independent fan profile card. Dialogs and persistence remain host
 * responsibilities, allowing this card to be embedded as a feature subset.
 */
@Composable
fun FanProfilesSection(
    state: FanProfileSectionState,
    onSelectCurve: () -> Unit,
    onEditCurve: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSectionTitle(
        text = stringResource(R.string.fanfeature_profiles),
        modifier = modifier.padding(bottom = SettingsTokens.sectionTitleBottomPadding),
    )
    val count = if (state.enabled) 2 else 1
    SettingsSegmentGroup {
        SettingsPreferenceRow(
            index = 0,
            count = count,
            title = stringResource(R.string.fanfeature_curve),
            summary = if (state.enabled) {
                state.activeCurveName
            } else {
                stringResource(R.string.fanfeature_off)
            },
            onClick = onSelectCurve,
        )
        if (state.enabled) {
            SettingsPreferenceRow(
                index = 1,
                count = count,
                title = stringResource(R.string.fanfeature_edit_curve),
                summary = pluralStringResource(
                    R.plurals.fanfeature_curve_points,
                    state.controlPointCount,
                    state.activeCurveName,
                    state.controlPointCount,
                ),
                onClick = onEditCurve,
            )
        }
    }
}

/**
 * Telemetry is represented by display-ready values, so this module has no
 * dependency on a particular sensor reader or root implementation.
 */
@Composable
fun FanTelemetrySection(
    state: FanTelemetrySectionState,
    onOverlayClick: () -> Unit,
    onOverlayEnabledChange: (Boolean) -> Unit,
    onCpuClick: () -> Unit,
    onGpuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSectionTitle(
        text = stringResource(R.string.fanfeature_live_telemetry),
        modifier = modifier.padding(bottom = SettingsTokens.sectionTitleBottomPadding),
    )
    SettingsSegmentGroup {
        SettingsPreferenceRow(
            index = 0,
            count = 2,
            title = stringResource(R.string.fanfeature_overlay),
            summary = stringResource(
                if (state.overlayPermissionGranted) {
                    R.string.fanfeature_overlay_summary
                } else {
                    R.string.fanfeature_overlay_permission_missing
                }
            ),
            onClick = onOverlayClick,
            trailingContent = {
                Switch(
                    checked = state.overlayEnabled && state.overlayPermissionGranted,
                    onCheckedChange = onOverlayEnabledChange,
                )
            },
        )
        TelemetryPanel(
            state = state,
            onCpuClick = onCpuClick,
            onGpuClick = onGpuClick,
        )
    }
}

@Composable
private fun TelemetryPanel(
    state: FanTelemetrySectionState,
    onCpuClick: () -> Unit,
    onGpuClick: () -> Unit,
) {
    val shape = ListItemDefaults.segmentedShapes(index = 1, count = 2).shape
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TemperatureTile(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.fanfeature_cpu),
                    state = state.cpu,
                    onClick = onCpuClick,
                )
                TemperatureTile(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.fanfeature_gpu),
                    state = state.gpu,
                    onClick = onGpuClick,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniMetric(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.fanfeature_memory),
                    value = state.memoryTemperature,
                )
                MiniMetric(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.fanfeature_battery),
                    value = state.batteryTemperature,
                )
            }
        }
    }
}

@Composable
private fun TemperatureTile(
    modifier: Modifier,
    title: String,
    state: TemperatureTileUiState,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.fanfeature_average, state.average),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.fanfeature_maximum, state.hottest),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MiniMetric(
    modifier: Modifier,
    title: String,
    value: String,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}
