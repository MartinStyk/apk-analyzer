# Apk Analyzer Agent Instructions

## Project Overview

Apk Analyzer is an Android multi-module application that lets users inspect installed apps and APK files on their device. It shows app details (permissions, activities, services, certificates), device-wide statistics, and permission usage across all installed apps.

* **Language** - Kotlin 2.4 (kotlin `2.4.10`, ksp `2.3.10`).
* **UI** - Jetpack Compose only. No XML layouts.
* **Dependency Injection** - Hilt `2.60.1`. No Dagger/Koin.
* **Libraries** - Use libraries from `gradle/libs.versions.toml`. Do not introduce new libraries unless required.
* **Concurrency** - Kotlin **coroutines** and **flows** exclusively.
* **Build System** - Gradle with convention plugins in `build-logic/`.
* **Android SDK** - compileSdk 37, targetSdk 37, minSdk 28.
* **JVM Toolchain** - Java 25.

## Project Structure

Every module listed below has its own `AGENTS.md` (dense, agent-oriented reference: purpose,
package, annotated structure, key interfaces, dependencies, known gotchas — not user
documentation) plus a one-line `CLAUDE.md` pointer. Read the specific module's `AGENTS.md` before
working inside it instead of re-deriving its structure from scratch.

### Modules

| Module | Gradle ID | Purpose |
|--------|-----------|---------|
| `app` | `:app` | Main Application class, `ApkAnalyzerActivity`, top-level Hilt wiring, navigation host |
| `core:common` | `:core:common` | `DispatcherProvider`, `PersistenceRepository` (DataStore), `ResourcesManager`, `Logger`, shared models (`AppSource`, `AppSize`), clipboard, digest utilities |
| `core:apps` | `:core:apps` | `InstalledAppsRepository`, `AppDetailRepository`, `StorageStatsRepository`, `UsageStatsRepository`, `PackageChangesObserver`, analysis utilities (`CertificateExtractor`, `ManifestParser`, `InstallSourceResolver`, `SdkVersionResolver`) |
| `core:app-permissions` | `:core:app-permissions` | `DevicePermissionsRepository`, `PermissionLabelProvider` for aggregating permission usage |
| `core:app-statistics` | `:core:app-statistics` | `LocalApplicationStatisticManager` for computing device-wide statistics |
| `core:user-preferences` | `:core:user-preferences` | `RecentlyViewedAppsRepository`, `SearchHistoryRepository` for user history/settings |
| `core:navigation` | `:core:navigation` | `NavigationState`, `Navigator`, `rememberNavigationState()`, `toEntries()` |
| `core:ui-library` | `:core:ui-library` | `ApkAnalyzerTheme`, `AppTheme`, `ApkAnalyzerIcons`, reusable Compose components, animations, modifiers |
| `feature:apps:api` | `:feature:apps:api` | `AppsNavKey` |
| `feature:apps:impl` | `:feature:apps:impl` | App list screen, search, filter, sort, permission filter |
| `feature:app-detail:api` | `:feature:app-detail:api` | `AppDetailNavKey`, `AppDetailInput` |
| `feature:app-detail:impl` | `:feature:app-detail:impl` | App detail screen, general info sub-screen |
| `feature:permissions:api` | `:feature:permissions:api` | `PermissionsNavKey` |
| `feature:permissions:impl` | `:feature:permissions:impl` | Permissions overview screen — **stub/placeholder, not yet implemented** |
| `feature:statistics:api` | `:feature:statistics:api` | `StatisticsNavKey` |
| `feature:statistics:impl` | `:feature:statistics:impl` | Statistics overview screen — **stub/placeholder, not yet implemented** |
| `feature:settings:api` | `:feature:settings:api` | `SettingsNavKey` |
| `feature:settings:impl` | `:feature:settings:impl` | Settings screen |

### Module Dependency Rules

* `feature/*/api` → depends on nothing. Contains `@Serializable` NavKey objects. Gets `navigation3-runtime` via `apkanalyzer.feature.api` plugin.
* `feature/*/impl` → `api(projects.feature.*.api)` + any needed `core` modules. Gets `:core:ui-library`, `:core:navigation`, Hilt, Compose via `apkanalyzer.feature.impl` plugin.
* `core` modules → can depend on other `core` modules (e.g., `core:apps` → `core:common`). Never depend on `feature` modules.
* `app` → depends on all `feature/*/impl` and all `core` modules.

### Package Structure

* Root: `sk.styk.martin.apkanalyzer`
* Feature: `sk.styk.martin.apkanalyzer.feature.<name>.api` / `sk.styk.martin.apkanalyzer.feature.<name>.impl`
* Core: `sk.styk.martin.apkanalyzer.core.<name>` (package name matches module namespace, e.g. `core.common`, `core.apps`, `core.uilibrary`, `core.apppermissions`, `core.appstatistics`, `core.userpreferences`)
* App: `sk.styk.martin.apkanalyzer.ui`, `.dependencyinjection`, `.manager`, `.util`

## Convention Plugins (build-logic)

| Plugin ID | Implementation | What it does |
|-----------|---------------|-------------|
| `apkanalyzer.library` | `LibraryPlugin` | Android library + spotless + compileSdk/minSdk + Kotlin config |
| `apkanalyzer.application` | `ApplicationPlugin` | Android app + Firebase (Analytics, Crashlytics, Performance) + release config |
| `apkanalyzer.feature.api` | `FeatureApiPlugin` | library + serialization + `navigation3-runtime` dependency |
| `apkanalyzer.feature.impl` | `FeatureImplPlugin` | library + hilt + compose + `:core:ui-library` + `:core:navigation` |
| `apkanalyzer.hilt` | `HiltPlugin` | Hilt + KSP compiler |
| `apkanalyzer.compose` | `ComposePlugin` | Compose compiler + BOM + `compose` bundle + `navigation3` bundle + serialization |
| `apkanalyzer.spotless` | `SpotlessPlugin` | ktlint + compose-rules-ktlint, auto-applied by `apkanalyzer.library` |

## Key Dependencies (from libs.versions.toml)

| Category | Libraries |
|----------|-----------|
| Compose | BOM `2026.06.01`, foundation `1.11.4`, material3, material-icons-extended |
| Navigation 3 | `navigation3-runtime` `1.1.4`, `navigation3-ui`, `lifecycle-viewmodel-navigation3`, `hilt-navigation-compose` |
| Hilt | `2.60.1` |
| Firebase | BOM `34.16.0` (analytics, crashlytics, performance) |
| Lifecycle | `2.11.0` (runtime-ktx, viewmodel-ktx, runtime-compose, process) |
| Kotlin | `kotlinx-collections-immutable` `0.5.1`, kotlinx-serialization |
| Image | Coil 3 (`coil-compose` `3.5.0`) |
| Logging | Timber `5.0.1` |
| Debug | LeakCanary `2.14` |
| Formatting | Spotless `8.8.0` + compose-rules-ktlint `0.6.3` |

## Coding Guidelines

### Navigation

* **Navigation 3** (`androidx.navigation3`) exclusively. No legacy Jetpack Navigation.
* Navigation keys are `@Serializable` object/data class implementing `NavKey`, placed in `feature/*/api` modules.
* Keys with parameters use `data class` (e.g., `AppDetailNavKey(val detailInput: AppDetailInput)`).
* Internal navigation keys (within a feature sub-graph) are placed in `feature/*/impl/navigation/` (e.g., `AppFilterNavKey`, `AppSearchNavKey`).
* Screen entry registration uses `EntryProviderScope<NavKey>.featureEntries(navigator: Navigator)` extension functions in `feature/*/impl/navigation/` packages.
* Top-level navigation uses `NavigationState` + `Navigator` from `:core:navigation`.
* In `app` module, `ApkAnalyzerApp.kt` wires all entry providers via `entryProvider { }` block.
* Entry transition metadata: use `bottomEntryMetadata()`, `slideFromEndEntryMetadata()` from `core:uilibrary:animation`.
* Top-level destinations defined in `app/ui/navigation/TopLevelDestinations.kt` using `NavigationBarItem`.

### Hilt Dependency Injection

* ViewModels: annotate with `@HiltViewModel`, inject via `@Inject constructor`.
* Assisted injection: use `@HiltViewModel(assistedFactory = VM.Factory::class)` + `@AssistedFactory` interface + `@AssistedInject constructor` (see `AppDetailViewModel`).
* Hilt modules: use `@Module @InstallIn(SingletonComponent::class)`. Prefer `interface` with `@Binds` for interfaces. Use `class` with `@Provides` for platform types.
* Use `@Singleton` for repository/manager bindings.
* Constructor injection with `@Inject constructor()` is preferred over module `@Provides`.

### MVVM Architecture

#### ViewModel Pattern
* Extend `ViewModel()` directly. No base class.
* Expose `val state: StateFlow<FeatureState>` built with `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue)` or a simple `MutableStateFlow` with backing field.
* Events (one-shot): use `Channel<Event>(Channel.BUFFERED)` exposed as `eventChannel.receiveAsFlow()`.
* Actions: single `fun onAction(action: FeatureAction)` method with `when` dispatch.
* In Composable: collect state with `collectAsStateWithLifecycle()`, collect events via `LaunchedEffect`.

#### State, Event, Action Pattern
* **State** - `sealed interface` or `@Immutable data class` + `StateFlow`. No lambdas in State.
* **Event** - `sealed interface`. One-off VM-to-UI signals (navigation, toasts, system intents).
* **Action** - `sealed interface`. UI-to-VM intents dispatched via `onAction()`.

#### Data Layer
* **Repository** - Data retrieval, persistence. Interface + `Impl` class in same module. Bound via Hilt `@Binds`.
* **Manager** - Complex business logic. Same pattern.
* No exceptions across interfaces. Use `Result<T>`, nullable `T?`, or empty collections.
* `DispatcherProvider` injected for dispatcher switching. Use `flowOn(dispatcherProvider.default())` or `withContext(dispatcherProvider.io())`.

#### File Structure
```
feature/<name>/impl/
  navigation/FeatureEntryProvider.kt  (+ inner NavKeys if not top-level)
  list/                               (or root package for simple features)
    FeatureScreen.kt
    FeatureViewModel.kt
    FeatureState.kt
    FeatureAction.kt
    FeatureEvent.kt
  components/                         (feature-specific reusable components)
  domain/                             (feature-local use cases/repositories)
```

### UI Library & Material Usage

* All Material3 components are wrapped in `:core:ui-library` and re-exported: `Button`, `Checkbox`, `Chip`, `Switch`, `Text`, `Toolbar`, `NavigationBar`, `SearchBarActive`, `SearchBarInactive`, `BottomSheet`, `LoadingSpinner`, `SkeletonBox`, `Icon`, `IconButton`, `AppIcon`, `RangeSlider`, `DateRangePickerDialog`.
* Feature modules must **not** import `androidx.compose.material3` directly. Only use Compose foundation APIs + `:core:ui-library`.
* Theme: access via `AppTheme.colors` and `AppTheme.typography` from `core:uilibrary:theme`.
* Icons: `ApkAnalyzerIcons` object in `core:uilibrary:icons`.
* Shared transitions: `LocalSharedTransitionScope` CompositionLocal in `core:uilibrary:modifier`.
* Lazy list utilities in `core:uilibrary:lazylist`.

### Compose Stability & Collections

* `kotlinx.collections.immutable` (`ImmutableList`, `persistentListOf`) for list properties in State and Composable parameters.
* `@Immutable` on State data classes.
* `@Stable` on non-data classes used as Composable parameters.

### Compose Previews

* Every file with `@Composable` functions includes `@Preview` functions.
* Wrap in `ApkAnalyzerTheme { }`.
* Preview functions are `private` and suffixed with `Preview`.
* Use realistic sample data. Don't preview ViewModel-dependent screen composables.

### Logging

* Use `Logger` from `core:common:logger` (not raw Timber).
* Pattern: `Logger.d("Tag", "message")`, `Logger.e("Tag", throwable, "message")`.
* Define `private const val TAG = "ClassName"` at file level for ViewModel/Manager tags.

### Serialization

* **Kotlin Serialization** for navigation keys (`@Serializable`).
* **Parcelize** for legacy Android-specific data (intents/bundles). Newer models use `@Serializable`.

### Style & Conventions

* Official Kotlin coding conventions.
* `data object` instead of plain `object` for sealed interface members (e.g., `data object Loading : State`).
* No wildcard imports.
* Never write comments or KDoc in generated code — not even when the WHY seems non-obvious. Prefer self-documenting names/structure instead. Only exception: the user explicitly asks for a comment to be added in that specific instance.
* Prefer `private` visibility; `internal` for module-visible.
* `public` only for actual public API.
* Spotless: `./gradlew spotlessApply` before committing.
* Ktlint config: multiline signatures at 3+ params, compose rules enabled.
* Git commits: always authored as the human user only. Never add `Co-Authored-By: Claude`, `Claude-Session:`, or any AI co-author trailer. See `.claude/skills/git-commit-author/SKILL.md`.

### Naming Conventions

* **camelCase** - functions, variables, properties.
* **PascalCase** - classes, interfaces, objects, enum values, `@Composable` functions.
* **UPPER_SNAKE_CASE** - constants.
* Callbacks in Composable: `on<Action>` (present tense, never past tense like `onClicked`).

## Unit Testing

* No tests exist yet. When adding tests:
* **MockK** for mocking.
* **Turbine** for Flow testing.
* **kotlinx-coroutines-test** (`runTest`) for coroutine testing.
* Place in `src/test/kotlin/` mirroring main source package.

## String Resources & Copywriting

* Write for non-technical users who understand Android basics.
* Concise, complete, plain English.
* Active voice, present tense.
* Be specific — no vague phrases.
* Technically accurate re: Android concepts.

## Skills

**You MUST read the relevant skill file before performing any of these tasks.** Skills contain step-by-step instructions, templates, and checklists.

| Skill | Slash Command | SKILL.md Path | When to Use |
|-------|--------------|---------------|-------------|
| Create Feature Module | `/create-feature-module` | `.claude/skills/create-feature-module/SKILL.md` | Creating a new feature (api + impl modules, NavKey, entry provider, wiring) |
| Create Core Module | `/create-core-module` | `.claude/skills/create-core-module/SKILL.md` | Creating a new core/shared library module (repository, manager, utilities) |
| Create Compose Component | `/create-compose-component` | `.claude/skills/create-compose-component/SKILL.md` | Adding a reusable UI component to `:core:ui-library` |
| Implement Navigation | `/implement-navigation` | `.claude/skills/implement-navigation/SKILL.md` | Adding a screen destination, wiring navigation, entry provider patterns |
| Spotless Fix | `/spotless-fix` | `.claude/skills/spotless-fix/SKILL.md` | Fixing formatting/ktlint errors, running spotless |
| Git Commit Author | `/git-commit-author` | `.claude/skills/git-commit-author/SKILL.md` | Any git commit — enforces human-only authorship, no AI trailers |
| Setup Local Tools | `/setup-local-tools` | `.claude/skills/setup-local-tools/SKILL.md` | Setting up a new machine, checking dev tools (`gh`, JDK, Android SDK, Firebase CLI) |
| Analyze CI Failure | `/analyze-ci-failure` | `.claude/skills/analyze-ci-failure/SKILL.md` | Diagnosing a failed GitHub Actions run (`android.yml` / `android-publish.yml`) |
| Run App | `/run-app` | `.claude/skills/run-app/SKILL.md` | Building, installing, and launching the app on a connected device/emulator |
| Sync Design Changes | `/sync-design-changes` | `.claude/skills/sync-design-changes/SKILL.md` | Translating tweaks made in the Claude Design ("Apk Analyzer Design System") project back into Kotlin/Compose code |

## Code Templates

### ViewModel with Assisted Injection

```kotlin
@HiltViewModel(assistedFactory = MyViewModel.Factory::class)
class MyViewModel @AssistedInject constructor(
    @Assisted private val input: InputType,
    private val repository: MyRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(input: InputType): MyViewModel
    }
}
```

### Standard ViewModel Pattern

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val eventChannel = Channel<MyEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val state = repository.data()
        .map { it.toState() }
        .flowOn(dispatcherProvider.default())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MyState.Loading)

    fun onAction(action: MyAction) {
        when (action) {
            is MyAction.ItemClicked -> { /* handle */ }
        }
    }
}
```

