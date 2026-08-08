# app Module

## Purpose
The top-level Android application module. No business logic of its own — wires together every `core/*` and `feature/*/impl` module, hosts the launcher and external-APK document activities, defines their navigation graphs, and provides app-scoped analytics and lifecycle bindings.

## Package: `sk.styk.martin.apkanalyzer` (root — no module suffix, unlike `core.*`/`feature.*`)

Sub-packages: `.dependencyinjection`, `.manager.analytics`, `.ui` (`.externalapk`, `.navigation`), `.util.file`.

## Structure

```
ApkAnalyzer.kt                          - Application class (@HiltAndroidApp); Coil SingletonImageLoader.Factory; registers injected Set<DefaultLifecycleObserver> multibindings onto ProcessLifecycleOwner
dependencyinjection/
  ApplicationModule.kt                  - @InstallIn(SingletonComponent) @Module: app-scoped CoroutineScope and FirebaseAnalytics
  ActivityCommonModule.kt               - @InstallIn(ActivityComponent) @Module @ActivityScoped: Activity Context and ComponentActivity
manager/
  analytics/
    AnalyticsTracker.kt                 - Wraps FirebaseAnalytics; AppAction enum (SHOW_MANIFEST, EXPORT_APK, SAVE_ICON, OPEN_SYSTEM_ABOUT, OPEN_GOOGLE_PLAY) + trackScreenView()
ui/
  ApkAnalyzerActivity.kt                - @AndroidEntryPoint launcher Activity; observes ApkAnalyzerViewModel.state for color scheme and hosts ApkAnalyzerApp
  ApkAnalyzerThemeHost.kt               - Shared Compose theme/night-mode host used by both activities
  externalapk/                           - Document-task Activity, URI-copy ViewModel, and standalone Navigation 3 host for APKs opened by other apps
  ApkAnalyzerApp.kt                     - Top-level @Composable: NavigationState + Navigator (core:navigation), Scaffold + bottom NavigationBar (core:ui-library), SharedTransitionLayout + entryProvider wiring every feature's nav graph, NavDisplay
  ApkAnalyzerState.kt                   - sealed interface: Loading / Data(colorAppScheme: ColorAppScheme)
  ApkAnalyzerViewModel.kt               - @HiltViewModel; maps PersistenceRepository.observe(Key.ColorScheme) into ApkAnalyzerState via stateIn(WhileSubscribed(5000)) — exists solely so the Activity can drive day/night mode before the Compose theme renders
  navigation/
    TopLevelDestinations.kt             - internal TOP_LEVEL_DESTINATIONS (persistentListOf<NavigationBarItem>): Apps, Browse tabs with icons/titles; internal TOP_LEVEL_KEYS
util/
  ViewModelExtensions.kt                - Legacy pre-Hilt ViewModelProvider.Factory helper extensions
  file/
    GenericFileProvider.kt              - FileProvider subclass; companion AUTHORITY = "sk.styk.martin.apkanalyzer" (wired as ${applicationId} in the manifest)
```

## How `ApkAnalyzerApp.kt` wires every feature together

```kotlin
@Composable
internal fun ApkAnalyzerApp() {
    val navigationState = rememberNavigationState(startKey = AppsNavKey, topLevelKeys = TOP_LEVEL_KEYS)
    val navigator = remember { Navigator(navigationState) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                items = TOP_LEVEL_DESTINATIONS,
                selectedKey = navigationState.currentTopLevelKey,
                isVisible = true,
                onSelectKey = navigator::navigate,
            )
        },
    ) { paddings ->
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                val entryProvider = entryProvider {
                    appEntries(navigator)
                    appDetailEntries(navigator)
                    browseEntries()
                    settingsEntries(navigator)
                }
                NavDisplay(
                    modifier = Modifier.fillMaxWidth().padding(paddings),
                    entries = navigationState.toEntries(entryProvider),
                    onBack = navigator::goBack,
                )
            }
        }
    }
}
```

`AppsNavKey` is the start destination. Each `*Entries()` call comes from the corresponding
`feature/*/impl/navigation/*EntryProvider.kt` and only takes a `navigator` param when that
feature navigates *out* to another feature (apps, app-detail, settings do; browse
currently doesn't since it's a stub screen — see its own `AGENTS.md`). **Adding a new
feature/screen means adding its `*Entries()` call here** — see the `create-feature-module` and
`implement-navigation` skills.

## Manifest Highlights (`app/src/main/AndroidManifest.xml`)

- **Permissions:** `QUERY_ALL_PACKAGES` (lint-suppressed), `PACKAGE_USAGE_STATS` (lint-suppressed, protected permission).
- **Application:** `android:name=".ApkAnalyzer"`, `allowBackup=true`, `hardwareAccelerated=true`, `largeHeap=true`, `supportsRtl=true`.
- **Launcher activity:** `.ui.ApkAnalyzerActivity` — `exported=true`, `windowSoftInputMode=adjustResize`, `MAIN`/`LAUNCHER`.
- **External APK activity:** `.ui.externalapk.ExternalApkActivity` — `exported=true`, `documentLaunchMode=always`, excluded from recents, and handles `VIEW`/`INSTALL_PACKAGE` for `.apk` files in an isolated document task.
- **Provider:** `.util.file.GenericFileProvider`, authority `${applicationId}`, `exported=false`, `grantUriPermissions=true`.
- **Meta-data:** `google_analytics_automatic_screen_reporting_enabled = false` — screen views are tracked manually via `AnalyticsTracker.trackScreenView`, not GA's automatic tracking.

## Dependencies

Plugins: `apkanalyzer.application`, `apkanalyzer.hilt`, `apkanalyzer.compose`, `apkanalyzer.spotless`, `parcelize`.

`projects.*`: all of `core.apps`, `core.appPermissions`, `core.appIndex`, `core.common`, `core.userPreferences`, `core.navigation`, `core.uiLibrary`, and all of `feature.apps.impl`, `feature.browse.impl`, `feature.settings.impl`, `feature.appDetail.impl`.

Key libraries: `androidx.activity.compose`, `androidx.appcompat`, `androidx.lifecycle.viewmodel.ktx`/`.runtime.compose`/`.process` (for `ProcessLifecycleOwner`), `androidx.compose.material3`, `kotlinx.collections.immutable`, `coil.compose`, `debugImplementation(libs.leakcanary)`.

`applicationId = "sk.styk.martin.apkanalyzer"`. `versionCode`/`versionName` come from
`version.code`/`version.name` Gradle properties. `gradle.properties` supplies local defaults (`1` and
`dev`); workflows override them with `-Pversion.code=`/`-Pversion.name=`. Continuous integration
changes only `version.name`, while the release workflow derives both values from the pushed semantic
version tag.
