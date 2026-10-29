# feature:apps Module

## Purpose

The start destination for browsing installed apps, searching, filtering, sorting, opening APK files,
and navigating to app details or settings.

The API module exposes only the top-level navigation key. The phone implementation uses the package
`sk.styk.martin.apkanalyzer.feature.apps.phone`; the TV one is described under [TV Module](#tv-module).
Opening APK files and navigating to settings are phone-only.

## Package Map

* `list/` - the installed-app list and recently viewed section.
* `search/` - app search and search-history interaction.
* `filter/` - the phone filter screens. The filter state, rules, and quick filters live in `core:apps`
  `filtering/`; `filter/domain/` keeps only the nested-screen draft plumbing. `filter/permission/`,
  `filter/source/`, and `filter/sdkversion/` each own a nested full-screen multiselect destination
  opened from a card on the main filter screen.
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

## TV Module

`feature/apps/tv` (package `sk.styk.martin.apkanalyzer.feature.apps.tv`) is the TV app list: a
focusable grid of installed apps with the recently viewed row on top. It has its own `TvApps*`
ViewModel and never depends on `phone`. Unlike the phone, TV shows recently viewed under every sort,
because typing with a remote is slow and the row costs only one line above the grid. Like the phone,
it hides the row while filters are active, so everything on screen matches the filter.

* On entry, focus goes to the last focused card, or on first launch the first recently viewed app
  (the first installed app when there are none), so nothing scrolls before the user moves; never
  the side menu. Card keys carry their section (`recent:` / `installed:`) because one app can appear
  in both.
* Selecting an app opens `AppDetailNavKey`, the same key the phone uses.
* The list has a fixed header (title, app count, Search, Sort, Filters) above a scrolling grid of
  `AppCard` tiles tinted with each app's color; when sorted by anything but name, each tile shows its
  sort value. Sorting lives in a side panel opened from the Sort button. Each row has three states: off, then its natural direction on the
  first press (name and target SDK ascending, sizes and dates largest/newest first), then reversed on
  every further press, so changing direction never means scrolling to a separate control. Every dialog or panel on the screen is one `TvAppsOverlay` value, so two can
  never show at once.
* Sorting uses the shared `AppSortType` comparator from `core:apps`. Total size and last used need
  usage access: without it they open a rationale dialog, and they are hidden entirely on a device
  with no usage access settings screen (`UsageStatsRepository.canRequestPermission`).
* TV opens usage access settings as a side panel that only pauses the activity, so the
  process-level `onStart` refresh in `core:apps` never fires. The screen re-checks access on resume
  and applies the sort the user was waiting for.
* Search (`search/`, internal `TvAppSearchNavKey`) uses the shared `search` matcher from
  `core:apps` and the same `SearchHistoryRepository` as the phone. The keyboard opens only on first
  entry; returning from app detail restores focus to the opened result. The keyboard's search key
  closes the keyboard and focuses the first result; with no results, focus stays in the field.
* Filters (`filter/`, internal `TvAppFilterNavKey`) are one screen of chip rows that edit
  `AppFilterRepository` directly, with no draft or Apply step; the match count updates live. Size
  and date ranges are reduced to the quick options because picking ranges with a remote is
  impractical. Usage-based options appear only with usage access, since without it every app's
  last-used time is unknown and the results would be wrong.
* Every TV screen restores focus through `rememberRestorableFocus` from `core:design-system:tv`: give each
  focusable a stable key that carries its section, and list the keys currently on screen so a
  vanished item falls back to the default instead of requesting focus on nothing.

## Navigation Topology

The apps destination opens search and filter internally. Filter opens permission selection. App
details and settings are cross-feature destinations and must use their API modules.

Register all internal destinations through this feature's entry provider, then register that provider
once in its app host — `appEntries` in `ApkAnalyzerApp.kt`, `tvAppEntries` in `TvApkAnalyzerApp.kt`.

## State and Ownership Rules

* Filtering and sorting happen before the UI receives list state.
* Filter state is the in-memory `AppFilterRepository` from `core:apps`, shared by every filter
  surface on a device.
* Recently viewed data remains optional and comes from `core:user-preferences`.
* The APK picker owns temporary-file acquisition until ownership transfers to app detail. Preserve
  release behavior for cancellation, failed navigation, and Activity teardown.
* Usage and storage permission rationale state is modeled by distinct sealed variants because the
  system settings destinations and explanations differ.
