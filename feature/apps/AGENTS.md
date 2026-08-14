# feature:apps Module

## Purpose

The start destination for browsing installed apps, searching, filtering, sorting, opening APK files,
and navigating to app details or settings.

The API module exposes only the top-level navigation key. Implementation code uses the package
`sk.styk.martin.apkanalyzer.feature.apps.impl`.

## Package Map

* `list/` - the installed-app list and recently viewed section.
* `search/` - app search and search-history interaction.
* `filter/` - in-memory filter state and quick filters. `filter/permission/`, `filter/source/`, and
  `filter/sdkversion/` each own a nested full-screen multiselect destination opened from a card on
  the main filter screen.
* `components/` - feature-owned reusable UI, including APK document selection and app rows.
* `navigation/` - the top-level and internal Navigation 3 destinations.

Each user-visible destination owns its State/Action/Event/ViewModel set. Do not centralize search,
filter, and nested-filter-screen state into the list ViewModel.

A section on the main filter screen that opens a nested multiselect destination (permission, source,
SDK version) edits `FilterViewModel`'s uncommitted `localFilter` draft, never `AppFilterRepository`
directly — the nested screen and the main screen's inline chips must stay in sync with the same
draft, and the top-level Apply/unsaved-changes flow stays authoritative. Each nested destination is
bridged in through its own `Singleton` coordinator (`PermissionFilterCoordinator`,
`SourceFilterCoordinator`, `SdkVersionFilterCoordinator`) built on the shared `FilterDraftBridge`,
because the nested screen is a separate Navigation 3 destination with its own `HiltViewModel` and
cannot share `FilterViewModel`'s in-memory state directly.

## Navigation Topology

The apps destination opens search and filter internally. Filter opens permission selection. App
details and settings are cross-feature destinations and must use their API modules.

Register all internal destinations through this feature's entry provider, then register that provider
once in the app host.

## State and Ownership Rules

* Filtering and sorting happen before the UI receives list state.
* Filter state is an in-memory domain repository shared by the feature's filter surfaces.
* Recently viewed data remains optional and comes from `core:user-preferences`.
* The APK picker owns temporary-file acquisition until ownership transfers to app detail. Preserve
  release behavior for cancellation, failed navigation, and Activity teardown.
* Usage and storage permission rationale state is modeled by distinct sealed variants because the
  system settings destinations and explanations differ.
