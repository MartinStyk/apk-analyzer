# Apk Analyzer Copilot Instructions

## Project Overview

Apk Analyzer is an Android multi-module application. Follow these project-specific standards:

* **Language** - Kotlin 2.3 for all Android development.
* **UI** - Jetpack Compose only. Avoid XML layouts.
* **Dependency Injection** - Hilt. Avoid Dagger or Koin.
* **Libraries** - Use libraries from `libs.version.toml`. Do not introduce new libraries unless required.
* **Concurrency** - Kotlin **coroutines** and **flows** exclusively.

## Project Structure

Project uses a multi-module architecture divided into following modules:

* **`app`** - Contains main Application class, `ApkAnalyzerActivity` and top level application components, such as Hilt dependency graphs.
* **`feature`** (multiple modules) - UI screens & logic related to a specific feature.
  * Feature modules are split to 2 modules:
    * `api` - navigation keys
    * `impl` - implementation of the UI layer
* **`core`** (multiple modules) - Domain logic, reusable components, utilities that can be shared across multiple features or other libraries.
  * `common` - `DispatcherProvider`, `PersistenceRepository` (DataStore), `ResourcesManager`, `Logger` (Timber + Firebase Crashlytics wrapper). Base dependency for most modules.
  * `app-analysis-core` - Android `PackageManager`-based app analysis: `AppGeneralDataManager`, `AppPermissionManager`, `CertificateManager`, `AndroidManifestManager`. Data models like `GeneralData`, `PermissionData`, `AppSource`.
  * `app-list` - `InstalledAppsRepository` for querying installed apps. Returns `LazyAppListData`.
  * `app-permissions` - `LocalPermissionManager` for aggregating permission usage across all installed apps.
  * `app-statistics` - `LocalApplicationStatisticManager` for computing device-wide app statistics.
  * `navigation` - `NavigationState`, `Navigator`, `rememberNavigationState()` for Navigation 3 top-level navigation.
  * `ui-library` - `ApkAnalyzerTheme`, `ApkAnalyzerIcons`, reusable Compose components (`NavigationBar`), color scheme, typography.

### Module Dependency Rules

* `feature/*/api` - depends on nothing except `:core:navigation` (via convention plugin). Contains only `NavKey` objects.
* `feature/*/impl` - depends on its own `api` module (via `api(projects.feature.*.api)`), plus any `core` modules needed.
* `core` modules - can depend on other `core` modules (e.g., `core:app-list` → `core:app-analysis-core` → `core:common`). Never depend on `feature` modules.
* `app` - depends on all `feature/*/impl` and all `core` modules. The only module that wires everything together.

### Package Structure

* Root package: `sk.styk.martin.apkanalyzer`
* Feature packages: `sk.styk.martin.apkanalyzer.feature.<name>.api` / `.impl`
* Core packages: `sk.styk.martin.apkanalyzer.core.<name>`
* App packages: `sk.styk.martin.apkanalyzer.ui`, `.dependencyinjection`, `.manager`, `.util`

## Coding Guidelines

### Navigation

* **Navigation 3** (`androidx.navigation3`) for all screen navigation. Do NOT use legacy Jetpack Navigation (`androidx.navigation.compose`).
* Navigation keys are `@Serializable object`s implementing `NavKey`, placed in `feature/*/api` modules.
* Screen entry registration uses `EntryProviderScope<NavKey>.featureEntries()` extension functions in `feature/*/impl/navigation/` packages.
* Top-level navigation uses a custom `NavigationState` + `Navigator` from `:core:navigation`.

### Convention Plugins (build-logic)

When creating new modules, use these convention plugins in `build.gradle.kts`:

* `apkanalyzer.library` - base Android library setup (compileSdk, minSdk, Kotlin).
* `apkanalyzer.application` - app module setup (Firebase, release config).
* `apkanalyzer.feature.api` - feature API module (library + serialization + navigation3 runtime).
* `apkanalyzer.feature.impl` - feature impl module (library + hilt + compose + `:core:ui-library` dependency).
* `apkanalyzer.hilt` - adds Hilt + KSP compiler.
* `apkanalyzer.compose` - adds Compose compiler + BOM + compose/navigation3 bundles.
* `apkanalyzer.spotless` - adds ktlint formatting via Spotless.

### Hilt Dependency Injection

* ViewModels: annotate with `@HiltViewModel`, inject via `@Inject constructor`.
* Hilt modules: use `@Module @InstallIn(SingletonComponent::class)`. Prefer `interface` with `@Binds` for binding interfaces to implementations. Use `class` with `@Provides` for platform types.
* Use `@Singleton` for repository/manager bindings. Use `@ActivityScoped` only for activity-specific dependencies.
* Constructor injection with `@Inject constructor()` is preferred over module `@Provides` where possible.

### MVVM Architecture

#### Component Responsibilities
* **Composable** - Presenter layer. Reacts to `state`, collects `event` via `LaunchedEffect`, and delegates user interaction to VM. Callback naming: `on<Action>` or `goTo<Destination>`.
* **ViewModel** - Extends `ViewModel()` directly (no custom BaseViewModel). Combines internal flows into a single `state`. Communicates with Core layer (Managers/Repositories).

#### ViewModel Pattern
* Expose `val state: StateFlow<FeatureState>` built with `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue)`.
* In Activity/Composable: collect state with `collectAsStateWithLifecycle()`.
* Events (one-shot): use `SharedFlow` or `Channel`.

#### State, Event, Action
* **State** - Current component status. Use `sealed interface/class` + `StateFlow`. Mark data classes `@Immutable`. **No lambdas in State**; use `Action` instead.
* **Event** - One-off VM-to-UI signals (e.g., navigation). Use `sealed interface`.
* **Action** - UI-to-VM intents. Use `sealed interface`.

#### Data Layer
* **Repository** - Responsible for data retrieval, persistence, and source abstraction.
* **Manager** - Responsible for complex business logic operations and implementation details.

#### File Structure
Keep feature-related components together in a single package:
`Feature.kt`, `FeatureViewModel.kt`, `FeatureState.kt`, `FeatureAction.kt`, `FeatureEvent.kt`.

### Compose Stability

* Use `kotlinx.collections.immutable` (`ImmutableList`, `persistentListOf`) for list properties in State classes and Composable parameters.
* Use `@Stable` annotation on non-data classes used as Composable parameters.
* Use `@Immutable` on State data classes.

### Standard Libraries

* **Timber** - for logging (via `Logger` wrapper in `core:common`).
* **DataStore Preferences** - for persisting user settings (`PersistenceRepository`).
* **Firebase** - Analytics, Crashlytics, Performance. Set up via `apkanalyzer.application` convention plugin.
* **LeakCanary** - debug-only memory leak detection.
* **Kotlinx Serialization** - for navigation key serialization.
* **Kotlinx Collections Immutable** - for Compose-stable collections.

### Logging

* Use `Logger` from `core:common` (not raw `Timber`). It wraps Timber and Firebase Crashlytics.
* Always provide a tag string as the first parameter: `Logger.d("FeatureName", "message")`.
* Available levels: `v`, `d`, `i`, `w`, `e`. Error/warning variants accept `Throwable`: `Logger.e("Tag", throwable, "message")`.

### Serialization

* **Kotlin Serialization** - for navigation keys (`@Serializable`). Prefer over `Parcelable` for new code.
* **Parcelize** - legacy models use `@Parcelize`. Acceptable for Android-specific data passed via intents/bundles.

### Style & Conventions

* Follow official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
* Use `data object` instead of standard `object`.
* Spotless - Ensure that all code adheres to the spotless-compatible formatting rules. Run `./gradlew spotlessApply` to auto-fix formatting before committing.
* Avoid using wildcard imports. Always import specific classes or functions.
* Do not generate comments, KDoc, or Javadoc unless explicitly requested. The code should be self-explanatory.
* When logging, use `Logger` with a tag. Do not use raw `Timber`.
* Functions and properties should be public only if they are part of the public API. Otherwise, prefer private visibility, or internal visibility if function should be visible within the module.

### Naming Conventions

* Use **camelCase** for function names, variable names, and property names.
* Use **PascalCase** for class names, interface names, object names, enum values and `@Composable` function names.
* Use **UPPER_SNAKE_CASE** for constants.

## Unit Testing

* No tests exist yet. When adding tests, follow these conventions:
* Use **MockK** for mocking.
* Use **Turbine** for Flow testing.
* Use **kotlinx-coroutines-test** (`runTest`) for coroutine testing.
* Place tests in `src/test/kotlin/` mirroring the main source package structure.
