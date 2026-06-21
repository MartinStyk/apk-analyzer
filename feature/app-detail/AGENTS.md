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
AppDetailScreen.kt           - Main detail screen Composable
AppDetailViewModel.kt        - Uses @HiltViewModel with AssistedFactory for AppDetailInput
AppDetailState.kt            - Loading/Loaded/Error states with full app detail data
AppDetailAction.kt           - User actions (retry, view manifest, export, navigate sections)
AppDetailEvent.kt            - Navigation/system events
components/
  AppDetailBadge.kt          - Badge classification (Sideloaded, DangerousPermissions, Unused, Large, System, etc.)
  (other detail UI components)
generalinfo/                 - General info sub-screen
```

## Key Patterns
- Uses **Assisted Injection** (`@HiltViewModel(assistedFactory = ...)`) because the ViewModel requires `AppDetailInput` at creation time.
- `AppDetailState.Loaded` is a large data class with all detail fields (no nested loading).
- Badge computation uses `AppClassificationThresholds` from `core:apps`.

## Dependencies
- `core:apps` (AppDetailRepository)
- `kotlinx-collections-immutable`
- `coil-compose`

