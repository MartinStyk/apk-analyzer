# feature:apps Module

## Purpose
The primary feature — displays the list of installed apps with filtering, sorting, searching, and recently-viewed apps. This is the start destination of the app.

## Sub-modules
- `api` - Contains `AppsNavKey` (top-level destination)
- `impl` - Full implementation

## Package: `sk.styk.martin.apkanalyzer.feature.apps.impl`

## Structure

```
navigation/
  AppEntryProvider.kt      - appEntries(navigator) - registers AppsNavKey, AppSearchNavKey, AppFilterNavKey, PermissionFilterNavKey
  AppFilterNavKey.kt       - Internal nav key for filter screen
  AppSearchNavKey.kt       - Internal nav key for search screen
  PermissionFilterNavKey.kt - Internal nav key for permission filter
list/
  AppsScreen.kt            - Main app list Composable
  AppsViewModel.kt         - @HiltViewModel, combines installed apps + filter + sort + recents
  AppsState.kt             - AppsState, AppListState, RecentsState, AppListItem, SortType
  AppsAction.kt            - User actions (sort, click, filter, search, settings)
  AppsEvent.kt             - Navigation events
search/
  AppSearchScreen.kt       - Search overlay screen
  AppSearchState/Action/Event/ViewModel.kt - Own State/Action/Event/ViewModel, same MVI shape as list/
  domain/
    SearchAppsUseCase.kt   - Applies search query to app list
filter/
  FilterScreen.kt          - Filter bottom sheet / screen
  FilterState/Action/Event/ViewModel.kt - Own State/Action/Event/ViewModel, same MVI shape as list/
  domain/
    AppFilterRepository.kt - In-memory filter state management
    AppFilterState.kt      - Filter criteria data class
    FilterAppsUseCase.kt   - Applies filter to app list
    PermissionFilterCoordinator.kt, PermissionPreset.kt, QuickFilter.kt, UnusedAppsPeriod.kt - Supporting filter domain types
  permission/
    PermissionFilterScreen.kt - Permission-based filtering
    PermissionFilterState/Action/Event/ViewModel.kt - Own State/Action/Event/ViewModel
components/
  apkfilepicker/                     - Reusable APK document-picker button with its own MVI ViewModel and temporary-file ownership
  PermissionRationaleBottomSheet.kt - Defines AppDataPermission (sealed interface: UsageAccess, StorageAccess — NOT an enum) + the rationale bottom sheet UI
  AppsSkeletons.kt         - Loading skeleton placeholders
  appitem/AppListItemRow.kt - Single app row in the list
  quickfilter/QuickFilterRow*.kt - Quick-filter chip row
  (other shared components)
```

Every sub-screen (`list/`, `search/`, `filter/`, `filter/permission/`) follows the same
State/Action/Event/ViewModel MVI shape as the top-level `AppsViewModel` — not just a single
screen with helper composables.

## Key Dependencies
- `core:apps` (InstalledAppsRepository, StorageStatsRepository, UsageStatsRepository)
- `core:user-preferences` (RecentlyViewedAppsRepository)
- `core:app-permissions` (for permission filter)
- `feature:settings:api` (SettingsNavKey for navigation)
- `feature:app-detail:api` (AppDetailNavKey, AppDetailInput for navigation)
- `kotlinx-collections-immutable`
- `coil-compose`

## Navigation Flow
```
AppsNavKey → AppSearchNavKey (fade out transition)
AppsNavKey → AppFilterNavKey (bottom entry)
AppFilterNavKey → PermissionFilterNavKey (slide from end)
AppsNavKey → AppDetailNavKey (via navigator)
AppsNavKey → SettingsNavKey (via navigator)
```
