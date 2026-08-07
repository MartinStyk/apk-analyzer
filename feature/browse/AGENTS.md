# feature:browse Module

## Purpose
Bottom-navigation top-level destination for "Browse by Attribute" (tab label "Browse") — roadmap
`CE-05`: pick a dimension, see its bucket counts, tap a bucket to see the apps in it. **Status:
stub/placeholder — not yet implemented.** Currently renders only centered "Browse" text, no state, no
ViewModel, no logic. Do not assume this feature has real behavior — check before referencing it in
other work. Replaces the former `feature:permissions` and `feature:statistics` stub tabs, per
`docs/product/roadmap.md` §1.1b's module consolidation note.

## Sub-modules
- `api` — Contains `BrowseNavKey` (top-level destination) and the "Browse" string resource used as
  the bottom-nav tab label
- `impl` — Placeholder screen only

## Package: `sk.styk.martin.apkanalyzer.feature.browse.impl`

## API Module Key Types

```kotlin
@Serializable
object BrowseNavKey : NavKey
```

## Impl Structure

```
navigation/
  BrowseEntryProvider.kt  - browseEntries(): EntryProviderScope<NavKey> extension, registers BrowseNavKey -> BrowseScreen()
BrowseScreen.kt            - @Composable stub: centered "Browse" text, with @Preview
```

Takes no `Navigator` parameter since the placeholder screen doesn't navigate anywhere.

## When Implementing This Feature

The real feature is expected to consume `core:app-index`'s `AppIndexRepository` (dimension → bucket →
package-name index, built off `InstalledAppsRepository` — see `core/app-index/AGENTS.md`) for the
dimension picker and bucket-count list, then navigate to `feature:app-detail`'s `AppDetailNavKey` per
tapped app. Follow the MVVM pattern used by `feature:settings` or `feature:apps` (State/Action/Event +
`@HiltViewModel`) rather than inventing a new shape.

## Dependencies
- `api`: `apkanalyzer.feature.api` plugin, no explicit dependencies
- `impl`: `apkanalyzer.feature.impl` plugin, `api(projects.feature.browse.api)` only
