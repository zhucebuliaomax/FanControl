@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.mmax.retrocontrol.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mmax.retrocontrol.R
import com.mmax.retrocontrol.data.ControlPreset
import com.mmax.retrocontrol.designsystem.SecondaryMenuList
import com.mmax.retrocontrol.designsystem.SecondaryMenuListItem
import com.mmax.retrocontrol.designsystem.SwipeToDeleteSecondaryMenuListItem
import com.mmax.retrocontrol.designsystem.bringIntoViewOnFocus

data class PresetListItemUiState(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val fanCurveName: String,
)

data class PresetFanCurveChoice(
    val id: String?,
    val name: String,
)

@Composable
fun PresetManagementSection(
    presets: List<PresetListItemUiState>,
    onPresetClick: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    itemModifier: (Int) -> Modifier = { Modifier },
) {
    SecondaryMenuList {
        presets.forEachIndexed { index, preset ->
            if (preset.isDefault) {
                PresetListItem(
                    preset = preset,
                    index = index,
                    count = presets.size,
                    onClick = { onPresetClick(preset.id) },
                    modifier = itemModifier(index),
                )
            } else {
                key(preset.id) {
                    SwipeToDeletePresetItem(
                        preset = preset,
                        index = index,
                        count = presets.size,
                        onClick = { onPresetClick(preset.id) },
                        onDelete = { onDeletePreset(preset.id) },
                        modifier = itemModifier(index),
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalPresetSelectionSection(
    presets: List<PresetListItemUiState>,
    selectedPresetId: String,
    onPresetSelected: (String) -> Unit,
    itemModifier: (Int) -> Modifier = { Modifier },
) {
    SecondaryMenuList {
        presets.forEachIndexed { index, preset ->
            val selected = preset.id == selectedPresetId
            SecondaryMenuListItem(
                index = index,
                count = presets.size,
                onClick = { onPresetSelected(preset.id) },
                colors = if (selected) {
                    ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        supportingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        trailingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    ListItemDefaults.segmentedColors()
                },
                content = {
                    Text(
                        text = preset.name,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = { Text(preset.fanCurveName) },
                trailingContent = {
                    if (selected) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                },
                modifier = itemModifier(index),
            )
        }
    }
}

@Composable
fun AddPresetButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text(stringResource(R.string.add_preset)) },
    )
}

@Composable
private fun PresetListItem(
    preset: PresetListItemUiState,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SecondaryMenuListItem(
        index = index,
        count = count,
        onClick = onClick,
        content = {
            Text(
                text = preset.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = { Text(preset.fanCurveName) },
        modifier = modifier,
    )
}

@Composable
private fun SwipeToDeletePresetItem(
    preset: PresetListItemUiState,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirmation by remember(preset.id) { mutableStateOf(false) }
    SwipeToDeleteSecondaryMenuListItem(
        index = index,
        count = count,
        onClick = onClick,
        onDeleteRequest = { showConfirmation = true },
        deleteIcon = Icons.Default.Delete,
        deleteContentDescription = stringResource(R.string.delete_preset),
        modifier = modifier,
        supportingContent = { Text(preset.fanCurveName) },
        content = {
            Text(
                text = preset.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
    if (showConfirmation) {
        DeletePresetConfirmation(
            name = preset.name,
            onConfirm = {
                showConfirmation = false
                onDelete()
            },
            onDismiss = { showConfirmation = false },
        )
    }
}

@Composable
fun PresetEditorDialog(
    preset: ControlPreset,
    fanCurveName: String,
    fanCurveChoices: List<PresetFanCurveChoice>,
    onFanCurveSelected: (String?) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var expandedSection by remember(preset.id) { mutableStateOf<String?>(null) }
    var showRename by remember(preset.id) { mutableStateOf(false) }
    var renameDraft by remember(preset.id, preset.name) { mutableStateOf(preset.name) }
    var showDelete by remember(preset.id) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showRename = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit_square),
                            contentDescription = stringResource(R.string.rename_preset),
                        )
                    }
                    if (!preset.isDefault) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = stringResource(R.string.delete_preset),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    ExpandablePresetSetting(
                        title = stringResource(R.string.preset_fan_curve),
                        summary = fanCurveName,
                        expanded = expandedSection == "fan",
                        hasChildren = fanCurveChoices.isNotEmpty(),
                        onToggle = {
                            expandedSection = if (expandedSection == "fan") null else "fan"
                        },
                        index = 0,
                        count = 4,
                    ) {
                        fanCurveChoices.forEachIndexed { index, choice ->
                            SegmentedListItem(
                                onClick = {
                                    onFanCurveSelected(choice.id)
                                    expandedSection = null
                                },
                                shapes = ListItemDefaults.segmentedShapes(
                                    index = index,
                                    count = fanCurveChoices.size,
                                ),
                                content = { Text(choice.name) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewOnFocus(),
                            )
                        }
                    }
                    EmptyPresetSetting(
                        title = stringResource(R.string.control_joystick),
                        index = 1,
                        count = 4,
                    )
                    EmptyPresetSetting(
                        title = stringResource(R.string.control_button_layout),
                        index = 2,
                        count = 4,
                    )
                    EmptyPresetSetting(
                        title = stringResource(R.string.preset_performance_profile),
                        index = 3,
                        count = 4,
                    )
                }
            }
        }
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text(stringResource(R.string.rename_preset)) },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.preset_name)) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRename(renameDraft)
                        showRename = false
                    },
                    enabled = renameDraft.isNotBlank(),
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    if (showDelete) {
        DeletePresetConfirmation(
            name = preset.name,
            onConfirm = onDelete,
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun ExpandablePresetSetting(
    title: String,
    summary: String,
    expanded: Boolean,
    hasChildren: Boolean,
    onToggle: () -> Unit,
    index: Int,
    count: Int,
    children: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        SegmentedListItem(
            onClick = { if (hasChildren) onToggle() },
            shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
            content = { Text(title) },
            supportingContent = { Text(summary) },
            trailingContent = {
                if (hasChildren) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 24.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                children()
            }
        }
    }
}

@Composable
private fun EmptyPresetSetting(
    title: String,
    index: Int,
    count: Int,
) {
    SegmentedListItem(
        onClick = {},
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        content = { Text(title) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DeletePresetConfirmation(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_preset)) },
        text = { Text(stringResource(R.string.delete_preset_confirmation, name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
