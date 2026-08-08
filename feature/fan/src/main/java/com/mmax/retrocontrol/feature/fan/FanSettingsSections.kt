@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.mmax.retrocontrol.feature.fan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mmax.retrocontrol.designsystem.SecondaryMenuList
import com.mmax.retrocontrol.designsystem.SecondaryMenuListItem
import com.mmax.retrocontrol.designsystem.SettingsSectionTitle
import com.mmax.retrocontrol.designsystem.SettingsTokens
import com.mmax.retrocontrol.designsystem.SwipeToDeleteSecondaryMenuListItem

data class FanProfileSectionState(
    val profiles: List<FanProfileItemUiState>,
)

data class FanProfileItemUiState(
    val id: String,
    val name: String,
    val controlPointCount: Int,
)

/**
 * Host-independent fan profile manager. Persistence and curve editing remain
 * host responsibilities, allowing the list to be embedded as a feature subset.
 */
@Composable
fun FanProfilesSection(
    state: FanProfileSectionState,
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

        val itemCount = state.profiles.size + 1
        SecondaryMenuList {
            FanProfileListItem(
                name = stringResource(R.string.fanfeature_off_title),
                summary = stringResource(R.string.fanfeature_off),
                index = 0,
                count = itemCount,
                onClick = {},
                modifier = offModifier,
            )
            state.profiles.forEachIndexed { index, profile ->
                key(profile.id) {
                    SwipeToDeleteProfileItem(
                        profile = profile,
                        index = index + 1,
                        count = itemCount,
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
        modifier = modifier,
    )
}

@Composable
private fun SwipeToDeleteProfileItem(
    profile: FanProfileItemUiState,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirmation by remember(profile.id) { mutableStateOf(false) }
    SwipeToDeleteSecondaryMenuListItem(
        index = index,
        count = count,
        onClick = onClick,
        onDeleteRequest = { showDeleteConfirmation = true },
        deleteIcon = Icons.Default.Delete,
        deleteContentDescription = stringResource(R.string.fanfeature_delete_curve),
        trailingContent = {
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        },
        modifier = modifier,
        supportingContent = {
            Text(
                stringResource(
                    R.string.fanfeature_control_points,
                    profile.controlPointCount,
                )
            )
        },
        content = {
            Text(
                text = profile.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.fanfeature_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.fanfeature_cancel))
                }
            },
        )
    }
}
