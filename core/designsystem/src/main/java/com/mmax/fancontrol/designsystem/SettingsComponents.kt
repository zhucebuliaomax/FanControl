@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.mmax.retrocontrol.designsystem

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FocusScrollMargin(
    margin: Dp = SettingsTokens.focusScrollMargin,
    content: @Composable () -> Unit,
) {
    val marginPx = with(LocalDensity.current) { margin.toPx() }
    val bringIntoViewSpec = remember(marginPx) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float,
            ): Float {
                val safeStart = marginPx
                val safeEnd = containerSize - marginPx
                val trailingEdge = offset + size
                return when {
                    offset < safeStart -> offset - safeStart
                    trailingEdge > safeEnd -> trailingEdge - safeEnd
                    else -> 0f
                }
            }
        }
    }
    CompositionLocalProvider(
        LocalBringIntoViewSpec provides bringIntoViewSpec,
        content = content,
    )
}

@Composable
fun Modifier.bringIntoViewOnFocus(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    return bringIntoViewRequester(requester)
        .onFocusChanged { state ->
            if (state.isFocused) {
                scope.launch { requester.bringIntoView() }
            }
        }
}

/**
 * Reusable settings primitives matching the native system settings grouping.
 *
 * Feature modules own their state and actions; these components own only visual
 * structure, so a host app can embed one section without importing this app.
 */
@Composable
fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(start = SettingsTokens.sectionTitleInset),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun SettingsSegmentGroup(
    modifier: Modifier = Modifier,
    content: @Composable SettingsSegmentScope.() -> Unit,
) {
    val scope = SettingsSegmentScope()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        scope.content()
    }
}

class SettingsSegmentScope internal constructor()

/** Standard container for lists shown inside second-level menu pages. */
@Composable
fun SecondaryMenuList(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        content()
    }
}

/**
 * Standard row for second-level menu lists.
 *
 * A one-row list is treated as a standalone card and always gets extra-large
 * idle corners. During an outer swipe gesture, [keepInteractionShape] prevents
 * focus loss from shrinking the corners.
 */
@Composable
fun SecondaryMenuListItem(
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    keepInteractionShape: Boolean = false,
    colors: ListItemColors? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shapes = secondaryMenuItemShapes(index, count).let { defaultShapes ->
        if (keepInteractionShape) defaultShapes.lockToInteractionShape() else defaultShapes
    }
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        colors = colors ?: ListItemDefaults.segmentedColors(),
        trailingContent = trailingContent,
        supportingContent = supportingContent,
        content = content,
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewOnFocus(),
    )
}

/** Standard bounded swipe-to-delete row for second-level menu lists. */
@Composable
fun SwipeToDeleteSecondaryMenuListItem(
    index: Int,
    count: Int,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit,
    deleteIcon: ImageVector,
    deleteContentDescription: String,
    modifier: Modifier = Modifier,
    supportingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.3f },
    )
    val scope = rememberCoroutineScope()
    val isSwipeInProgress by remember(dismissState) {
        derivedStateOf {
            abs(runCatching { dismissState.requireOffset() }.getOrDefault(0f)) > 0.5f
        }
    }
    val interactionShape = secondaryMenuItemShapes(index, count).focusedShape

    LaunchedEffect(dismissState.settledValue) {
        if (dismissState.settledValue != SwipeToDismissBoxValue.Settled) {
            onDeleteRequest()
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            SwipeDeleteAction(
                state = dismissState,
                onClick = {
                    onDeleteRequest()
                    scope.launch { dismissState.reset() }
                },
                shape = interactionShape,
                icon = deleteIcon,
                contentDescription = deleteContentDescription,
            )
        },
        content = {
            SecondaryMenuListItem(
                index = index,
                count = count,
                onClick = onClick,
                modifier = modifier,
                keepInteractionShape = isSwipeInProgress,
                supportingContent = supportingContent,
                content = content,
            )
        },
    )
}

@Composable
private fun secondaryMenuItemShapes(index: Int, count: Int): ListItemShapes =
    ListItemDefaults.segmentedShapes(
        index = index,
        count = count,
        defaultShapes = if (count == 1) {
            ListItemDefaults.shapes(shape = MaterialTheme.shapes.extraLarge)
        } else {
            ListItemDefaults.shapes()
        },
    )

@Composable
fun SettingsSegmentScope.SettingsPreferenceRow(
    index: Int,
    count: Int,
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    trailingIconContentDescription: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val defaultShapes = if (count == 1) {
        ListItemDefaults.shapes(shape = MaterialTheme.shapes.extraLarge)
    } else {
        ListItemDefaults.shapes()
    }
    SegmentedListItem(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewOnFocus(),
        shapes = ListItemDefaults.segmentedShapes(
            index = index,
            count = count,
            defaultShapes = defaultShapes,
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        trailingContent = trailingContent ?: trailingIcon?.let { icon ->
            {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = trailingIconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        supportingContent = summary?.let { supportingText ->
            {
                Text(
                    text = supportingText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        content = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
        },
    )
}

/**
 * Standard trailing action revealed when a segmented list item is swiped.
 *
 * The action grows with the reveal instead of stopping at a fixed maximum width,
 * matches the row height, and leaves the same gap used between segmented rows.
 */
@Composable
fun SwipeDeleteAction(
    state: SwipeToDismissBoxState,
    onClick: () -> Unit,
    shape: Shape,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val isActionVisible by remember(state) {
        derivedStateOf {
            abs(runCatching { state.requireOffset() }.getOrDefault(0f)) > 0.5f
        }
    }
    val actionWidth by remember(state, density) {
        derivedStateOf {
            val revealWidth = with(density) {
                abs(runCatching { state.requireOffset() }.getOrDefault(0f)).toDp()
            }
            (revealWidth - ListItemDefaults.SegmentedGap).coerceAtLeast(40.dp)
        }
    }

    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isActionVisible) {
            Button(
                onClick = onClick,
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight(),
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                )
            }
        }
    }
}

/** Keeps a row on one shape while an outer swipe container owns the gesture. */
fun ListItemShapes.lockToInteractionShape(shape: Shape = focusedShape): ListItemShapes = copy(
    shape = shape,
    selectedShape = shape,
    pressedShape = shape,
    focusedShape = shape,
    hoveredShape = shape,
    draggedShape = shape,
)

object SettingsTokens {
    val focusScrollMargin = 40.dp
    val pageHorizontalPadding = 16.dp
    val sectionTitleInset = 16.dp
    val sectionTitleBottomPadding = 8.dp
    val sectionGap = 22.dp
    val pageBottomPadding = 40.dp
}
