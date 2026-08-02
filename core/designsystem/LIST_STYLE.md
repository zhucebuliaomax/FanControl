# List item style

Use these rules for settings-style lists throughout the app.

## Standalone items

- A row that is visually its own card always uses `MaterialTheme.shapes.extraLarge`.
- Pass that shape through `ListItemDefaults.shapes(...)` as the `defaultShapes` of
  `ListItemDefaults.segmentedShapes(...)`. This keeps the large radius in the idle
  state as well as during focus, press, and selection animations.
- Do not let a standalone row inherit the small inner corners of a larger segmented
  list merely because its data shares the same collection.

## Swipe-to-delete actions

- Use `SwipeDeleteAction` from `SettingsComponents.kt` rather than implementing a
  feature-local swipe background.
- The delete action expands left with the revealed area and has no fixed maximum
  width, so it can reach the list's left edge during a full swipe.
- Its height matches the list row exactly.
- The visual gap between the moving row and the delete action is
  `ListItemDefaults.SegmentedGap`, the same gap used between list rows.
- Use the list row's segmented shape for the delete action so its outer corners
  stay aligned with the row it belongs to.
- A revealed delete action uses the row's focused shape. While the swipe offset is
  non-zero, lock every foreground interaction shape to that same focused shape;
  losing focus during a drag must not shrink the row corners before release.
- Do not draw the delete action at zero offset. A focused row has transparent outer
  corners, and an idle action underneath would otherwise bleed through them.
- Use the default `SwipeToDismissBox` anchors and observe its settled value to open
  confirmation UI; do not veto state changes with deprecated `confirmValueChange`.

## Secondary-menu implementation

- Build second-level page lists with `SecondaryMenuList`.
- Use `SecondaryMenuListItem` for normal rows and
  `SwipeToDeleteSecondaryMenuListItem` for removable rows.
- These primitives own row gaps, single-row extra-large corners, focus scrolling,
  bounded swipe behavior, and interaction-shape locking. Feature code should only
  provide row content, state, and actions.
