# core:ui-library Module

## Purpose
Design system module providing all reusable Compose UI components, theme, icons, animations, and modifiers. This is the **only** module allowed to depend on `androidx.compose.material3`.

## Package: `sk.styk.martin.apkanalyzer.core.uilibrary`

## Structure

```
theme/
  Theme.kt          - ApkAnalyzerTheme, AppTheme (colors + typography access)
  Color.kt          - ApkAnalyzerColorPalette, Light/Dark color definitions
  Typography.kt     - ApkAnalyzerTypography, font setup
  Shapes.kt         - Shape definitions
components/
  AppIcon.kt        - App icon display (Coil image loading)
  BottomSheet.kt    - Modal bottom sheet wrapper
  Button.kt         - Primary/secondary button variants
  Checkbox.kt       - Checkbox component
  Chip.kt           - Filter/selection chips
  DateRangePickerDialog.kt - Date range picker
  Icon.kt           - Icon wrapper
  IconButton.kt     - Icon button wrapper
  LoadingSpinner.kt - Circular progress indicator
  NavigationBar.kt  - Bottom navigation bar (NavigationBarItem data class)
  RangeSlider.kt    - Range slider component
  SearchBarActive.kt  - Expanded search bar with text field
  SearchBarInactive.kt - Collapsed/clickable search bar
  SkeletonBox.kt    - Loading skeleton placeholder
  Switch.kt         - Toggle switch
  Text.kt           - Text component with theme typography
  Toolbar.kt        - Top app bar
icons/
  ApkAnalyzerIcons.kt - Icon constants (Apps, Permissions, Statistics, etc.)
  app/              - Custom vector icon assets
animation/
  NavEntryTransitions.kt - bottomEntryMetadata(), slideFromEndEntryMetadata()
lazylist/           - LazyList utilities
modifier/           - Custom modifiers, LocalSharedTransitionScope
util/               - Utility composables
```

## Key Exports

- `ApkAnalyzerTheme(isDarkTheme: Boolean, content: @Composable () -> Unit)` - Root theme wrapper
- `AppTheme.colors` - Current color palette
- `AppTheme.typography` - Current typography styles
- `ApkAnalyzerIcons` - Icon constants object
- `NavigationBar` / `NavigationBarItem` - Bottom nav implementation
- `LocalSharedTransitionScope` - CompositionLocal for shared element transitions

## Dependencies

- `androidx.compose.material3`
- `androidx.compose.ui:ui-text-google-fonts`
- `androidx.compose.material:material-icons-extended`
- `kotlinx-collections-immutable`
- `coil-compose`, `coil-core`
- `navigation3-ui`
- `projects.core.common`

## Rules

- All new Material3 component usage must be wrapped here before use in features.
- Components should accept theme-consistent defaults.
- Every component file must include `@Preview` functions.

