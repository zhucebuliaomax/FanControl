@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.mmax.retrocontrol.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mmax.retrocontrol.R
import com.mmax.retrocontrol.designsystem.bringIntoViewOnFocus
import com.mmax.retrocontrol.data.AppControlProfile
import com.mmax.retrocontrol.designsystem.SettingsSectionTitle

data class AppProfileChoice(
    val id: String?,
    val name: String,
)

private enum class AppProfilePicker {
    PRESET,
    FAN_CURVE,
    JOYSTICK,
    BUTTON_LAYOUT,
    PERFORMANCE_PROFILE,
}

@Composable
fun AppProfileSection(
    profile: AppControlProfile?,
    selectedPresetId: String,
    selectedPresetName: String,
    selectedFanCurveName: String?,
    presetChoices: List<AppProfileChoice>,
    fanCurveChoices: List<AppProfileChoice>,
    onPresetSelected: (String) -> Unit,
    onFanCurveSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picker by remember(profile?.packageName) { mutableStateOf<AppProfilePicker?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        AppProfileSettingRow(
            title = stringResource(R.string.app_profile_preset),
            summary = selectedPresetName,
            onClick = { picker = AppProfilePicker.PRESET },
            index = 0,
            count = 1,
        )

        Spacer(Modifier.height(24.dp))
        SettingsSectionTitle(text = stringResource(R.string.control_title))
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
            ListItemDefaults.SegmentedGap
        )) {
            AppProfileSettingRow(
                title = stringResource(R.string.control_fan),
                summary = selectedFanCurveName,
                onClick = { picker = AppProfilePicker.FAN_CURVE },
                index = 0,
                count = 4,
            )
            AppProfileSettingRow(
                title = stringResource(R.string.control_joystick),
                summary = null,
                onClick = { picker = AppProfilePicker.JOYSTICK },
                index = 1,
                count = 4,
            )
            AppProfileSettingRow(
                title = stringResource(R.string.control_button_layout),
                summary = null,
                onClick = { picker = AppProfilePicker.BUTTON_LAYOUT },
                index = 2,
                count = 4,
            )
            AppProfileSettingRow(
                title = stringResource(R.string.control_core),
                summary = null,
                onClick = { picker = AppProfilePicker.PERFORMANCE_PROFILE },
                index = 3,
                count = 4,
            )
        }
    }

    picker?.let { activePicker ->
        val title = when (activePicker) {
            AppProfilePicker.PRESET -> stringResource(R.string.select_preset)
            AppProfilePicker.FAN_CURVE -> stringResource(R.string.select_fan_curve)
            AppProfilePicker.JOYSTICK -> stringResource(R.string.control_joystick)
            AppProfilePicker.BUTTON_LAYOUT -> stringResource(R.string.control_button_layout)
            AppProfilePicker.PERFORMANCE_PROFILE -> stringResource(R.string.control_core)
        }
        val choices = when (activePicker) {
            AppProfilePicker.PRESET -> presetChoices
            AppProfilePicker.FAN_CURVE -> fanCurveChoices
            AppProfilePicker.JOYSTICK,
            AppProfilePicker.BUTTON_LAYOUT,
            AppProfilePicker.PERFORMANCE_PROFILE -> emptyList()
        }
        val selectedId = when (activePicker) {
            AppProfilePicker.PRESET -> selectedPresetId
            AppProfilePicker.FAN_CURVE -> profile?.fanCurveId
            AppProfilePicker.JOYSTICK -> profile?.joystickId
            AppProfilePicker.BUTTON_LAYOUT -> profile?.buttonLayoutId
            AppProfilePicker.PERFORMANCE_PROFILE -> profile?.performanceProfileId
        }
        AppProfileSelectionDialog(
            title = title,
            choices = choices,
            selectedId = selectedId,
            onSelected = { id ->
                when (activePicker) {
                    AppProfilePicker.PRESET -> id?.let(onPresetSelected)
                    AppProfilePicker.FAN_CURVE -> onFanCurveSelected(id)
                    AppProfilePicker.JOYSTICK,
                    AppProfilePicker.BUTTON_LAYOUT,
                    AppProfilePicker.PERFORMANCE_PROFILE -> Unit
                }
                picker = null
            },
            onDismiss = { picker = null },
        )
    }
}

@Composable
private fun AppProfileSettingRow(
    title: String,
    summary: String?,
    onClick: () -> Unit,
    index: Int,
    count: Int,
) {
    val supporting: (@Composable () -> Unit)? = summary?.let { value ->
        { Text(value) }
    }
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(
            index = index,
            count = count,
            defaultShapes = if (count == 1) {
                ListItemDefaults.shapes(shape = MaterialTheme.shapes.extraLarge)
            } else {
                ListItemDefaults.shapes()
            },
        ),
        content = { Text(title) },
        supportingContent = supporting,
        trailingContent = {
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        },
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewOnFocus(),
    )
}

@Composable
private fun AppProfileSelectionDialog(
    title: String,
    choices: List<AppProfileChoice>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.width(300.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(top = 18.dp, bottom = 8.dp)) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                if (choices.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_options_available),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        choices.forEach { choice ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clickable { onSelected(choice.id) }
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (choice.id == selectedId) {
                                        Icons.Default.RadioButtonChecked
                                    } else {
                                        Icons.Default.RadioButtonUnchecked
                                    },
                                    contentDescription = null,
                                    tint = if (choice.id == selectedId) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                                Spacer(Modifier.width(14.dp))
                                Text(choice.name, maxLines = 1)
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.cancel),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
