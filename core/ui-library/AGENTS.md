# core:ui-library Module

## Purpose
Design system module providing all reusable Compose UI components, theme, icons, animations, and modifiers. **Feature modules must never import `androidx.compose.material3` — they consume it only through the wrappers here.** (`app` is the one exception: it uses material3 directly for `Scaffold` and theme plumbing in `ApkAnalyzerActivity.kt`/`ApkAnalyzerApp.kt`.)

## Package: `sk.styk.martin.apkanalyzer.core.uilibrary`

## Structure

```
theme/
  Theme.kt          - ApkAnalyzerTheme, AppTheme (colors + typography access)
  Color.kt          - ApkAnalyzerColorPalette, Light/Dark color definitions
  Typography.kt     - ApkAnalyzerTypography, font setup
  Shapes.kt         - Shape definitions
components/          - See the component inventory below
icons/
  ApkAnalyzerIcons.kt - Icon constants (Apps, Permissions, Statistics, etc.)
  app/PackageIconFetcher.kt, PackageIconModule.kt - Coil fetcher for `AppReference` icons
animation/
  NavEntryTransitions.kt - bottomEntryMetadata(), slideFromEndEntryMetadata()
lazylist/
  ItemsIndexedWithPosition.kt - LazyList item helper carrying first/middle/last position
modifier/
  CardModifier.kt, CollapsingHeader.kt, CollapsingToolbar.kt, Shimmer.kt,
  SharedTransitionModifier.kt (LocalSharedTransitionScope)
util/
  Lerp.kt
```

## Component Inventory

One file per component in `components/`. **The composable's name does not always match its file
name** — check here before calling one:

| Composable | File |
|---|---|
| `AppIcon` | `AppIcon.kt` |
| `BottomSheet` | `BottomSheet.kt` |
| `Button`, `TextButton` | `Button.kt` |
| `Checkbox` | `Checkbox.kt` |
| `Chip`, `OutlinedChip` | `Chip.kt` |
| `DateRangePickerDialog` | `DateRangePickerDialog.kt` |
| `HashBox` | `HashBox.kt` |
| `Icon` | `Icon.kt` |
| `IconButton` | `IconButton.kt` |
| `LabeledHashBox` | `LabeledHashBox.kt` |
| `LoadingSpinner` | `LoadingSpinner.kt` |
| `MultiSelectorChip` | `MultiSelectorChip.kt` |
| `NavigationBar` (+ `NavigationBarItem` data class) | `NavigationBar.kt` |
| `PermissionRationaleBottomSheet` | `PermissionRationaleBottomSheet.kt` |
| `RangeSlider` | `RangeSlider.kt` |
| `SearchBarActive` | `SearchBarActive.kt` |
| **`InactiveSearchBar`** — note the inverted name | `SearchBarInactive.kt` |
| `SegmentedBarChart` (+ `BarChartSegment` data class) | `SegmentedBarChart.kt` |
| `SelectorChip` | `SelectorChip.kt` |
| `SkeletonBox` | `SkeletonBox.kt` |
| `Switch` | `Switch.kt` |
| `Tag` | `Tag.kt` |
| `Text` | `Text.kt` |
| `Toolbar` | `Toolbar.kt` |

## Key Exports

- `ApkAnalyzerTheme(isDarkTheme: Boolean, content: @Composable () -> Unit)` - Root theme wrapper
- `AppTheme.colors` - Current color palette
- `AppTheme.typography` - Current typography styles
- `ApkAnalyzerIcons` - Icon constants object
- `NavigationBar` / `NavigationBarItem` - Bottom nav implementation
- `LocalSharedTransitionScope` - CompositionLocal for shared element transitions

## Dependencies

- `androidx.compose.material3`
- `androidx.compose.material:material-icons-extended`
- `kotlinx-collections-immutable`
- `coil-compose`, `coil-core`
- `navigation3-ui`
- `projects.core.common`

## Rules

- All new Material3 component usage must be wrapped here before use in features.
- Components should accept theme-consistent defaults.
- Every component file must include `@Preview` functions.
- When a styled status `Chip` is interactive, use its `onClick`/`onLongClick` overload so the shared
  component owns semantics and a shape-clipped ripple. Do not add `Modifier.clickable` to the
  non-interactive overload at a feature call site.
- `Chip` and `Tag` are not interchangeable. `Chip` is for selectable/interactive filter chips
  (larger touch target, `labelLarge`/`labelMedium` text). `Tag` is a small non-interactive inline
  badge for a fact on a list row (`labelSmall`, tighter padding) — e.g. Exported, Launcher, a
  permission flag. Reach for `Tag` first when labeling a row; a second private copy of either shape
  belongs here, not in a feature module.
