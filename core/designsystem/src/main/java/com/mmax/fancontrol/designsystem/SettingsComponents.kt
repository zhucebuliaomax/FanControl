@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.mmax.retrocontrol.designsystem

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

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

@Composable
fun SettingsSegmentScope.SettingsPreferenceRow(
    index: Int,
    count: Int,
    title: String,
    summary: String,
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
        supportingContent = {
            Text(
                text = summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

object SettingsTokens {
    val focusScrollMargin = 30.dp
    val pageHorizontalPadding = 16.dp
    val sectionTitleInset = 16.dp
    val sectionTitleBottomPadding = 8.dp
    val sectionGap = 22.dp
    val pageBottomPadding = 40.dp
}
