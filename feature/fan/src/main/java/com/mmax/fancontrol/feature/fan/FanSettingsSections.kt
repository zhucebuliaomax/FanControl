@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.mmax.retrocontrol.feature.fan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mmax.retrocontrol.designsystem.SettingsPreferenceRow
import com.mmax.retrocontrol.designsystem.SettingsSectionTitle
import com.mmax.retrocontrol.designsystem.SettingsSegmentGroup
import com.mmax.retrocontrol.designsystem.SettingsTokens
import com.mmax.retrocontrol.designsystem.bringIntoViewOnFocus

data class FanProfileSectionState(
    val activeProfileId: String?,
    val profiles: List<FanProfileItemUiState>,
)

data class FanProfileItemUiState(
    val id: String,
    val name: String,
    val controlPointCount: Int,
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
 * Host-independent fan profile manager. Persistence and curve editing remain
 * host responsibilities, allowing the list to be embedded as a feature subset.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FanProfilesSection(
    state: FanProfileSectionState,
    onOffSelected: () -> Unit,
    onProfileSelected: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    showTitle: Boolean = true,
    modifier: Modifier = Modifier,
    offModifier: Modifier = Modifier,
    profileModifier: (Int) -> Modifier = { Modifier },
) {
    Column(modifier = modifier) {
        if (showTitle) {
            SettingsSectionTitle(
                text = stringResource(R.string.fanfeature_profiles),
                modifier = Modifier.padding(bottom = SettingsTokens.sectionTitleBottomPadding),
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Column {
                FanProfileListItem(
                    name = stringResource(R.string.fanfeature_off_title),
                    summary = stringResource(R.string.fanfeature_off),
                    selected = state.activeProfileId == null,
                    onClick = onOffSelected,
                    modifier = offModifier,
                )
                state.profiles.forEachIndexed { index, profile ->
                    SwipeToDeleteProfileItem(
                        profile = profile,
                        selected = profile.id == state.activeProfileId,
                        onClick = { onProfileSelected(profile.id) },
                        onDelete = { onDeleteProfile(profile.id) },
                        modifier = profileModifier(index),
                    )
                }
            }
        }
    }
}

@Composable
fun FanProfilesAddCurveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
            )
        },
        text = { Text(stringResource(R.string.fanfeature_add_curve)) },
    )
}

@Composable
private fun FanProfileListItem(
    name: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = summary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.focusProperties { canFocus = false },
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ),
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewOnFocus()
            .clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteProfileItem(
    profile: FanProfileItemUiState,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirmation by remember(profile.id) { mutableStateOf(false) }
    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                showDeleteConfirmation = true
            }
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.fanfeature_delete_curve),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        content = {
            FanProfileListItem(
                name = profile.name,
                summary = stringResource(
                    R.string.fanfeature_control_points,
                    profile.controlPointCount,
                ),
                selected = selected,
                onClick = onClick,
                modifier = modifier,
            )
        },
    )

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.fanfeature_delete_curve)) },
            text = {
                Text(
                    stringResource(
                        R.string.fanfeature_delete_curve_confirmation,
                        profile.name,
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                ) {
                    Text(stringResource(R.string.fanfeature_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.fanfeature_cancel))
                }
            },
        )
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
    overlayModifier: Modifier = Modifier,
    telemetryPanelModifier: Modifier = Modifier,
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
            modifier = overlayModifier,
            trailingContent = {
                Switch(
                    checked = state.overlayEnabled && state.overlayPermissionGranted,
                    onCheckedChange = onOverlayEnabledChange,
                    modifier = Modifier.focusProperties { canFocus = false },
                )
            },
        )
        TelemetryPanel(
            state = state,
            onCpuClick = onCpuClick,
            onGpuClick = onGpuClick,
            modifier = telemetryPanelModifier,
        )
    }
}

@Composable
private fun TelemetryPanel(
    state: FanTelemetrySectionState,
    onCpuClick: () -> Unit,
    onGpuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = ListItemDefaults.segmentedShapes(index = 1, count = 2).shape
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewOnFocus()
            .indication(interactionSource, LocalIndication.current)
            .focusable(interactionSource = interactionSource),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TemperatureTile(
                    modifier = Modifier
                        .weight(1f)
                        .focusProperties { canFocus = false },
                    title = stringResource(R.string.fanfeature_cpu),
                    state = state.cpu,
                    onClick = onCpuClick,
                )
                TemperatureTile(
                    modifier = Modifier
                        .weight(1f)
                        .focusProperties { canFocus = false },
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
