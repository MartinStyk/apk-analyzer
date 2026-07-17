# feature:settings Module

## Purpose
Settings screen: color scheme (theme) selection and a toggle for the "recently viewed apps" feature. A secondary (non-top-level) destination reached by explicit navigation — currently triggered from `feature:apps` — not from the bottom nav bar.

## Sub-modules
- `api` — Contains `SettingsNavKey` only, no string resources (tab label strings live in impl here, unlike `feature:permissions`/`feature:statistics` where the label lives in `api`)
- `impl` — Full implementation

## Package: `sk.styk.martin.apkanalyzer.feature.settings.impl`

## API Module Key Types

```kotlin
@Serializable
data object SettingsNavKey : NavKey
```
Note: `data object`, unlike `feature:permissions`/`feature:statistics` which use plain `object`.

## Impl Structure

```
navigation/
  SettingsEntryProvider.kt  - settingsEntries(navigator: Navigator): EntryProviderScope<NavKey> extension; registers SettingsNavKey -> SettingsScreen(onBack = { navigator.goBack() })
SettingsAction.kt           - sealed interface: ColorSchemeSelected, RecentlyViewedAppsToggled, NavigateBack
SettingsEvent.kt            - sealed interface: NavigateBack (one-shot)
SettingsState.kt            - @Immutable data class: colorScheme, recentlyViewedAppsEnabled
SettingsViewModel.kt        - @HiltViewModel
SettingsScreen.kt           - SettingsScreen (stateful), SettingsContent (stateless), AppearanceSection, AppsSection, 2 @Preview functions
res/values/strings.xml      - settings_title, settings_appearance, settings_color_scheme(_day/night/follow_system), settings_apps_section, settings_recently_viewed_toggle
```

## ViewModel

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val persistenceRepository: PersistenceRepository,  // core:common
) : ViewModel() {
    val state: StateFlow<SettingsState>   // combine(observe(Key.ColorScheme), observe(Key.RecentlyViewedAppsEnabled))
    val events: Flow<SettingsEvent>       // Channel(BUFFERED).receiveAsFlow()
    fun onAction(action: SettingsAction)
}
```

`PersistenceRepository` (`core:common`): `fun <T> observe(key: Key<T>): Flow<T>`, `suspend fun <T> get/save(key: Key<T>, value: T)`.

## Key Patterns

- Standard State/Action/Event MVI shape, same as `feature:apps`/`feature:app-detail`.
- `NavigateBack` is modeled as **both** an `Action` (user pressed back) and an `Event` (ViewModel translates it into a one-shot navigation signal the screen collects to call `onBack()`) rather than the entry provider calling `navigator.goBack()` directly from a UI callback — this keeps the ViewModel decoupled from `Navigator`. Follow this pattern for any new feature needing a back action.
- No repository/use-case of its own — delegates entirely to `core:common`'s generic key-value `PersistenceRepository`.
- Entry provider needs a `Navigator` parameter (unlike `feature:permissions`/`feature:statistics`) because Settings needs to pop back.

## Dependencies
- `api`: `apkanalyzer.feature.api` plugin, no dependencies block
- `impl`: `apkanalyzer.feature.impl` plugin, `api(projects.feature.settings.api)`, `implementation(projects.core.common)`
