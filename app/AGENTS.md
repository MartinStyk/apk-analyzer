# app Module

## Purpose
The top-level Android application module. No business logic of its own — wires together every `core/*` and `feature/*/impl` module, hosts the single `Activity`, defines the app-wide navigation graph, and provides app-scoped analytics and lifecycle bindings.

## Package: `sk.styk.martin.apkanalyzer` (root — no module suffix, unlike `core.*`/`feature.*`)

Sub-packages: `.dependencyinjection`, `.manager.analytics`, `.ui` (`.navigation`), `.util.file`.

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
  ApkAnalyzerActivity.kt                - @AndroidEntryPoint ComponentActivity, the app's one and only Activity; edge-to-edge, observes ApkAnalyzerViewModel.state for color scheme, drives AppCompatDelegate.setDefaultNightMode, wraps content in ApkAnalyzerTheme { ApkAnalyzerApp() }
  ApkAnalyzerApp.kt                     - Top-level @Composable: NavigationState + Navigator (core:navigation), Scaffold + bottom NavigationBar (core:ui-library), SharedTransitionLayout + entryProvider wiring every feature's nav graph, NavDisplay
  ApkAnalyzerState.kt                   - sealed interface: Loading / Data(colorAppScheme: ColorAppScheme)
  ApkAnalyzerViewModel.kt               - @HiltViewModel; maps PersistenceRepository.observe(Key.ColorScheme) into ApkAnalyzerState via stateIn(WhileSubscribed(5000)) — exists solely so the Activity can drive day/night mode before the Compose theme renders
  navigation/
    TopLevelDestinations.kt             - internal TOP_LEVEL_DESTINATIONS (persistentListOf<NavigationBarItem>): Apps, Permissions, Statistics tabs with icons/titles; internal TOP_LEVEL_KEYS
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
                    permissionEntries()
                    statisticsEntries()
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
feature navigates *out* to another feature (apps, app-detail, settings do; permissions, statistics
currently don't since they're stub screens — see their own `AGENTS.md`). **Adding a new
feature/screen means adding its `*Entries()` call here** — see the `create-feature-module` and
`implement-navigation` skills.

## Manifest Highlights (`app/src/main/AndroidManifest.xml`)

- **Permissions:** `QUERY_ALL_PACKAGES` (lint-suppressed), `PACKAGE_USAGE_STATS` (lint-suppressed, protected permission).
- **Application:** `android:name=".ApkAnalyzer"`, `allowBackup=true`, `hardwareAccelerated=true`, `largeHeap=true`, `supportsRtl=true`.
- **Activity:** `.ui.ApkAnalyzerActivity` — the only one, `exported=true`, `windowSoftInputMode=adjustResize`, `MAIN`/`LAUNCHER`. A second activity block (VIEW/INSTALL_PACKAGE intent filter for opening `.apk` files externally) exists but is commented out/disabled.
- **Provider:** `.util.file.GenericFileProvider`, authority `${applicationId}`, `exported=false`, `grantUriPermissions=true`.
- **Meta-data:** `google_analytics_automatic_screen_reporting_enabled = false` — screen views are tracked manually via `AnalyticsTracker.trackScreenView`, not GA's automatic tracking.

## Dependencies

Plugins: `apkanalyzer.application`, `apkanalyzer.hilt`, `apkanalyzer.compose`, `apkanalyzer.spotless`, `parcelize`.

`projects.*`: all of `core.apps`, `core.appPermissions`, `core.appStatistics`, `core.common`, `core.userPreferences`, `core.navigation`, `core.uiLibrary`, and all of `feature.apps.impl`, `feature.permissions.impl`, `feature.statistics.impl`, `feature.settings.impl`, `feature.appDetail.impl`.

Key libraries: `androidx.activity.compose`, `androidx.appcompat`, `androidx.lifecycle.viewmodel.ktx`/`.runtime.compose`/`.process` (for `ProcessLifecycleOwner`), `androidx.compose.material3`, `kotlinx.collections.immutable`, `coil.compose`, `debugImplementation(libs.leakcanary)`.

`applicationId = "sk.styk.martin.apkanalyzer"`. `versionCode`/`versionName` come from
`version.code`/`version.name` Gradle properties. `gradle.properties` supplies local defaults (`1` and
`dev`); workflows override them with `-Pversion.code=`/`-Pversion.name=`. Continuous integration
changes only `version.name`, while the release workflow derives both values from the pushed semantic
version tag.
