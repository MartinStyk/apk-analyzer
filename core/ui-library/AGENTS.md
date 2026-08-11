# core:ui-library Module

## Purpose and Boundary

The shared Compose design system: theme, icons, animation metadata, reusable components, modifiers,
and lazy-list helpers. The package is `sk.styk.martin.apkanalyzer.core.uilibrary`.

Feature modules must not import Material3 directly. Wrap new Material3 usage here before consuming it
from a feature. The `app` module is the only exception for shell-level `Scaffold` and theme plumbing.

## Package Map

* `theme/` - `ApkAnalyzerTheme`, `AppTheme`, colors, typography, and shapes.
* `components/` - reusable UI primitives and composed controls.
* `icons/` - `ApkAnalyzerIcons` and app-icon loading.
* `animation/` - Navigation 3 transition metadata.
* `modifier/` and `lazylist/` - shared interaction, layout, and list behavior.

Search `components/` by composable name before adding or calling a component. The notable naming
exception is `InactiveSearchBar`, which lives in `SearchBarInactive.kt`.

## Component Rules

* Components accept theme-consistent defaults and every component file includes private previews.
* Feature colors and typography come from `AppTheme`; icons come from `ApkAnalyzerIcons`.
* Interactive status `Chip` usage goes through the component's click and long-click overload so the
  shared component owns semantics and its shape-clipped ripple.
* `Chip` is an interactive selector with a full touch target. `Tag` is a compact, non-interactive
  inline fact on a row. Do not use them interchangeably.
* A second copy of a shared shape, control, loading state, or interaction pattern belongs here rather
  than in another feature.
