@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.mmax.retrocontrol.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import com.mmax.retrocontrol.R
import com.mmax.retrocontrol.designsystem.FocusScrollMargin

internal enum class DashboardDestination(
    @StringRes val label: Int,
    @DrawableRes val outlinedIcon: Int,
    @DrawableRes val filledIcon: Int,
) {
    TELEMETRY(
        R.string.nav_telemetry,
        R.drawable.nav_telemetry_outlined,
        R.drawable.nav_telemetry_filled,
    ),
    CONTROLS(
        R.string.nav_controls,
        R.drawable.nav_controls_outlined,
        R.drawable.nav_controls_filled,
    ),
    APPS(
        R.string.nav_apps,
        R.drawable.nav_apps_outlined,
        R.drawable.nav_apps_filled,
    ),
    ACCESS(
        R.string.nav_access,
        R.drawable.nav_access_outlined,
        R.drawable.nav_access_filled,
    ),
}

internal enum class ControlModule(@StringRes val label: Int) {
    PRESET(R.string.control_preset),
    FAN(R.string.control_fan),
    JOYSTICK(R.string.control_joystick),
    BUTTON_LAYOUT(R.string.control_button_layout),
    CORE(R.string.control_core),
}

/**
 * Landscape-first Material layout. Window size classes keep the list/detail
 * behavior usable when the same activity is resized or shown in multi-window.
 */
@Composable
internal fun AdaptiveDashboardScaffold(
    selectedDestination: DashboardDestination,
    onDestinationSelected: (DashboardDestination) -> Unit,
    selectedControl: ControlModule?,
    onControlSelected: (ControlModule?) -> Unit,
    navigationFocusRequesters: List<FocusRequester>,
    controlFocusRequesters: List<FocusRequester>,
    emptyDetailFocusRequester: FocusRequester,
    onNavigationFocused: () -> Unit,
    onContentFocused: () -> Unit,
    onDetailFocused: () -> Unit,
    telemetryContent: @Composable () -> Unit,
    fanContent: @Composable () -> Unit,
    fanAction: @Composable () -> Unit,
    accessContent: @Composable () -> Unit,
    footer: @Composable () -> Unit,
) {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val windowSizeClass = adaptiveInfo.windowSizeClass
    val showTwoControlPanes = windowSizeClass.isWidthAtLeastBreakpoint(
        WIDTH_DP_EXPANDED_LOWER_BOUND
    )
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
        adaptiveInfo
    )
    val isNavigationBar = navigationSuiteType == NavigationSuiteType.NavigationBar

    NavigationSuiteScaffold(
        navigationItems = {
            DashboardDestination.entries.forEachIndexed { index, destination ->
                val selected = destination == selectedDestination
                NavigationSuiteItem(
                    selected = selected,
                    onClick = { onDestinationSelected(destination) },
                    icon = {
                        Icon(
                            painter = painterResource(
                                if (selected) destination.filledIcon else destination.outlinedIcon
                            ),
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(destination.label),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier
                        .focusRequester(navigationFocusRequesters[index])
                        .onFocusChanged { if (it.isFocused) onNavigationFocused() }
                        .focusProperties {
                            if (isNavigationBar) {
                                left = if (index == 0) {
                                    FocusRequester.Cancel
                                } else {
                                    navigationFocusRequesters[index - 1]
                                }
                                right = if (index == DashboardDestination.entries.lastIndex) {
                                    FocusRequester.Cancel
                                } else {
                                    navigationFocusRequesters[index + 1]
                                }
                                up = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                            } else {
                                up = if (index == 0) {
                                    FocusRequester.Cancel
                                } else {
                                    navigationFocusRequesters[index - 1]
                                }
                                down = if (index == DashboardDestination.entries.lastIndex) {
                                    FocusRequester.Cancel
                                } else {
                                    navigationFocusRequesters[index + 1]
                                }
                                left = FocusRequester.Cancel
                                right = FocusRequester.Cancel
                            }
                        },
                    navigationSuiteType = navigationSuiteType,
                )
            }
        },
        navigationSuiteType = navigationSuiteType,
        navigationItemVerticalArrangement = Arrangement.Center,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            when (selectedDestination) {
                DashboardDestination.TELEMETRY -> DashboardPage(
                    title = stringResource(R.string.nav_telemetry),
                    onFocused = onContentFocused,
                    content = telemetryContent,
                )

                DashboardDestination.CONTROLS -> ControlsPage(
                    selectedControl = selectedControl,
                    onControlSelected = onControlSelected,
                    showTwoPanes = showTwoControlPanes,
                    controlFocusRequesters = controlFocusRequesters,
                    emptyDetailFocusRequester = emptyDetailFocusRequester,
                    onContentFocused = onContentFocused,
                    onDetailFocused = onDetailFocused,
                    fanContent = fanContent,
                    fanAction = fanAction,
                )

                DashboardDestination.APPS -> DashboardPage(
                    title = stringResource(R.string.nav_apps),
                    content = {},
                )

                DashboardDestination.ACCESS -> DashboardPage(
                    title = stringResource(R.string.nav_access),
                    onFocused = onContentFocused,
                ) {
                    accessContent()
                    Spacer(Modifier.size(32.dp))
                    footer()
                }
            }
        }
    }
}

@Composable
private fun DashboardPage(
    title: String,
    onFocused: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    FocusScrollMargin {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusGroup()
                .onFocusChanged { if (it.hasFocus) onFocused() }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 30.dp, vertical = 30.dp),
        ) {
            PageTitle(title)
            Spacer(Modifier.size(18.dp))
            Box(Modifier.widthIn(max = 720.dp)) {
                Column { content() }
            }
            Spacer(Modifier.size(30.dp))
        }
    }
}

@Composable
private fun ControlsPage(
    selectedControl: ControlModule?,
    onControlSelected: (ControlModule?) -> Unit,
    showTwoPanes: Boolean,
    controlFocusRequesters: List<FocusRequester>,
    emptyDetailFocusRequester: FocusRequester,
    onContentFocused: () -> Unit,
    onDetailFocused: () -> Unit,
    fanContent: @Composable () -> Unit,
    fanAction: @Composable () -> Unit,
) {
    if (showTwoPanes) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ControlListPane(
                selectedControl = selectedControl,
                onControlSelected = onControlSelected,
                focusRequesters = controlFocusRequesters,
                onFocused = onContentFocused,
                modifier = Modifier.width(264.dp),
            )
            AnimatedVisibility(
                visible = selectedControl != null,
                modifier = Modifier.weight(1f),
                enter = expandHorizontally(expandFrom = Alignment.Start),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start),
            ) {
                selectedControl?.let { control ->
                    ControlDetailPane(
                        control = control,
                        fanContent = fanContent,
                        fanAction = fanAction,
                        emptyDetailFocusRequester = emptyDetailFocusRequester,
                        onFocused = onDetailFocused,
                    )
                }
            }
        }
    } else if (selectedControl == null) {
        ControlListPane(
            selectedControl = null,
            onControlSelected = onControlSelected,
            focusRequesters = controlFocusRequesters,
            onFocused = onContentFocused,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        )
    } else {
        ControlDetailPane(
            control = selectedControl,
            fanContent = fanContent,
            fanAction = fanAction,
            emptyDetailFocusRequester = emptyDetailFocusRequester,
            onFocused = onDetailFocused,
            onBack = { onControlSelected(null) },
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun ControlListPane(
    selectedControl: ControlModule?,
    onControlSelected: (ControlModule) -> Unit,
    focusRequesters: List<FocusRequester>,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .focusGroup()
            .onFocusChanged { if (it.hasFocus) onFocused() }
            .padding(horizontal = 8.dp, vertical = 14.dp),
    ) {
        PageTitle(
            title = stringResource(R.string.control_title),
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.size(16.dp))
        ControlModuleRow(
            control = ControlModule.PRESET,
            index = 0,
            count = 1,
            selected = selectedControl == ControlModule.PRESET,
            modifier = Modifier
                .focusRequester(focusRequesters[ControlModule.PRESET.ordinal])
                .focusProperties {
                    up = FocusRequester.Cancel
                    down = focusRequesters[ControlModule.FAN.ordinal]
                    left = FocusRequester.Cancel
                    right = FocusRequester.Cancel
                },
            onClick = { onControlSelected(ControlModule.PRESET) },
        )
        Spacer(Modifier.size(12.dp))
        val standardControls = listOf(
            ControlModule.FAN,
            ControlModule.JOYSTICK,
            ControlModule.BUTTON_LAYOUT,
            ControlModule.CORE,
        )
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            standardControls.forEachIndexed { index, control ->
                ControlModuleRow(
                    control = control,
                    index = index,
                    count = standardControls.size,
                    selected = control == selectedControl,
                    modifier = Modifier
                        .focusRequester(focusRequesters[control.ordinal])
                        .focusProperties {
                            up = if (index == 0) {
                                focusRequesters[ControlModule.PRESET.ordinal]
                            } else {
                                focusRequesters[standardControls[index - 1].ordinal]
                            }
                            down = if (index == standardControls.lastIndex) {
                                FocusRequester.Cancel
                            } else {
                                focusRequesters[standardControls[index + 1].ordinal]
                            }
                            left = FocusRequester.Cancel
                            right = FocusRequester.Cancel
                        },
                    onClick = { onControlSelected(control) },
                )
            }
        }
    }
}

@Composable
private fun ControlModuleRow(
    control: ControlModule,
    index: Int,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ),
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
        content = {
            Text(
                text = stringResource(control.label),
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        },
    )
}

@Composable
private fun ControlDetailPane(
    control: ControlModule,
    fanContent: @Composable () -> Unit,
    fanAction: @Composable () -> Unit,
    emptyDetailFocusRequester: FocusRequester,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .focusGroup()
            .onFocusChanged { if (it.hasFocus) onFocused() },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        FocusScrollMargin {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 30.dp,
                            top = 30.dp,
                            end = 30.dp,
                            bottom = if (control == ControlModule.FAN) 112.dp else 30.dp,
                        ),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.control_back),
                                )
                            }
                            Spacer(Modifier.size(4.dp))
                        }
                        PageTitle(stringResource(control.label))
                    }
                    if (control == ControlModule.FAN) {
                        Spacer(Modifier.size(18.dp))
                        fanContent()
                    } else {
                        Spacer(
                            Modifier
                                .size(1.dp)
                                .focusRequester(emptyDetailFocusRequester)
                                .focusProperties {
                                    up = FocusRequester.Cancel
                                    down = FocusRequester.Cancel
                                    left = FocusRequester.Cancel
                                    right = FocusRequester.Cancel
                                }
                                .focusable()
                        )
                    }
                }
                if (control == ControlModule.FAN) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(30.dp),
                    ) {
                        fanAction()
                    }
                }
            }
        }
    }
}

@Composable
private fun PageTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
