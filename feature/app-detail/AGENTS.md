# feature:app-detail Module

## Purpose
Displays detailed information about a single app (installed package or APK file). Shows general info, permissions, components, certificates, features, with sub-navigation to detail sections.

## Sub-modules
- `api` - Contains `AppDetailNavKey(detailInput: AppDetailInput)` and `AppDetailInput` sealed interface
- `impl` - Full implementation

## Package: `sk.styk.martin.apkanalyzer.feature.appdetail.impl`

## API Module Key Types

```kotlin
@Serializable
data class AppDetailNavKey(val detailInput: AppDetailInput) : NavKey

@Serializable
sealed interface AppDetailInput {
    @Serializable data class InstalledPackage(val packageName: String) : AppDetailInput
    @Serializable data class ApkFile(val apkFilePath: String) : AppDetailInput
}
```

## Impl Structure

```
navigation/
  AppDetailEntryProvider.kt  - appDetailEntries(navigator)
  GeneralInfoNavKey.kt       - Internal nav key for general info sub-screen
  PermissionsNavKey.kt       - Internal nav key for the permissions sub-screen
AppDetailScreen.kt           - Main detail screen Composable
AppDetailViewModel.kt        - Uses @HiltViewModel with AssistedFactory for AppDetailInput
AppDetailState.kt            - Loading/Loaded/Error states with full app detail data
AppDetailAction.kt           - User actions (retry, view manifest, export, navigate sections)
AppDetailEvent.kt            - Navigation/system events
components/
  AppDetailBadge.kt          - Badge classification (Sideloaded, DangerousPermissions, Unused, Large, System, etc.)
  AppDetailToolbar.kt        - Collapsing toolbar for the hub
  InfoRowItem.kt             - InfoRow + InfoRowItem + RationaleBottomSheet, shared by every sub-screen
  ScopeSelectorChip.kt       - OutlinedChip + BottomSheet that selects one of several scopes
generalinfo/                 - General info sub-screen
permissions/                 - Permissions sub-screen (see below)
```

Each sub-screen directory carries its own State/Action/Event/ViewModel/Screen set, same MVI shape
as the hub.

### `permissions/`

```
PermissionsScreen.kt              - Pinned toolbar, collapsing filter header, sectioned list
PermissionDetailBottomSheet.kt    - The item sheet: every raw field for one permission, tap a field to copy
PermissionResources.kt            - Enum -> @StringRes / icon mapping, kept out of the screen
PermissionsViewModel.kt           - Assisted-injected; combines a loaded source with the active narrowing
PermissionsState.kt               - Loading/Error/Loaded plus PermissionItem, PermissionSection and the
                                    PermissionScope / ProtectionLevel / ProtectionFlag / PermissionFilter
                                    / GrantState enums
PermissionsAction.kt              - Retry, ChangeQuery, SelectScope, ToggleFilter, ClearNarrowing, CopyValue
PermissionsEvent.kt               - ShowCopiedFeedback
PermissionDescriptionProvider.kt  - @Singleton; curated string -> system loadDescription -> declaring package
```

Narrowing (query, scope, property filters) lives in the ViewModel, not the Composable — the screen
receives only the already-filtered sections. Scope is a chip that opens a bottom sheet rather than a
tab row, and it renders only when the app defines permissions of its own. Grant pills and the
Granted / Denied filters render only in `InstalledPackage` mode.

## Key Patterns
- Uses **Assisted Injection** (`@HiltViewModel(assistedFactory = ...)`) because the ViewModel requires `AppDetailInput` at creation time.
- `AppDetailState.Loaded` is a large data class with all detail fields (no nested loading).
- Badge computation uses `AppClassificationThresholds` from `core:apps`.
- Sub-screens follow the `GeneralInfoScreen` idiom: **tap = explain, long-press = copy**.

## Design Doc

[`docs/product/features/app-detail.md`](../../docs/product/features/app-detail.md) is the approved
design for the remaining sub-screens (Components, Certificates, Requirements, hub rework). Read it
before adding one.

## Dependencies
- `core:apps` (AppDetailRepository)
- `core:app-permissions` (PermissionLabelProvider)
- `kotlinx-collections-immutable`
- `coil-compose`

