# core:navigation Module

## Purpose
Provides the custom Navigation 3 infrastructure for multi-stack (bottom-nav style) navigation.

## Package: `sk.styk.martin.apkanalyzer.core.navigation`

## Key Classes

### `NavigationState`
Manages a top-level stack (for bottom nav tabs) and per-tab sub-stacks.
- `currentTopLevelKey: NavKey` - Currently active top-level tab
- `currentKey: NavKey` - Currently visible screen (deepest in current sub-stack)
- `topLevelKeys: Set<NavKey>` - All registered top-level keys
- `currentSubStack: NavBackStack<NavKey>` - Stack for the current tab

### `rememberNavigationState(startKey: NavKey, topLevelKeys: List<NavKey>): NavigationState`
Composable factory. Creates one `NavBackStack` per top-level key + one top-level stack.

### `NavigationState.toEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): SnapshotStateList<NavEntry<NavKey>>`
Converts all sub-stacks into decorated nav entries for use with `NavDisplay`.

### `Navigator`
Imperative navigation controller wrapping `NavigationState`.
- `navigate(key: NavKey)` - Smart routing: top-level switch, same-tab reset, or sub-stack push.
- `goBack()` - Pop current sub-stack or go back to previous top-level tab.

## Usage Pattern
```kotlin
val navigationState = rememberNavigationState(startKey = AppsNavKey, topLevelKeys = TOP_LEVEL_KEYS)
val navigator = remember { Navigator(navigationState) }

NavDisplay(
    entries = navigationState.toEntries(entryProvider),
    onBack = navigator::goBack,
)
```

## Dependencies
- `apkanalyzer.library` + `apkanalyzer.compose` plugins
- Navigation 3 runtime + UI, lifecycle-viewmodel-navigation3

