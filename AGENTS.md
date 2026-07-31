# Apk Analyzer Agent Instructions

## Project Overview

Apk Analyzer is an Android multi-module application that lets users inspect installed apps and APK files on their device. It shows app details (permissions, activities, services, certificates), device-wide statistics, and permission usage across all installed apps.

* **Language** - Kotlin. Compiler and KSP versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).
* **UI** - Jetpack Compose only. No XML layouts.
* **Dependency Injection** - Hilt. No Dagger/Koin.
* **Libraries** - Use libraries from `gradle/libs.versions.toml`. Do not introduce new libraries unless required.
* **Concurrency** - Kotlin **coroutines** and **flows** exclusively.
* **Build System** - Gradle with convention plugins in `build-logic/`.
* **Android SDK** - Levels are centralized in [`AndroidSdk.kt`](build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/utils/AndroidSdk.kt).
* **JVM Toolchain** - Configured in [`Kotlin.kt`](build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/utils/Kotlin.kt) and [`gradle-daemon-jvm.properties`](gradle/gradle-daemon-jvm.properties).

## Project Structure

Every module listed below has its own `AGENTS.md` (dense, agent-oriented reference: purpose,
package, annotated structure, key interfaces, dependencies, known gotchas — not user
documentation) plus a one-line `CLAUDE.md` pointer. Read the specific module's `AGENTS.md` before
working inside it instead of re-deriving its structure from scratch.

### Shared AI Context

* `AGENTS.md` files are the canonical repository instructions. Put guidance in the closest relevant
  `AGENTS.md`; do not copy it into tool-specific files.
* `CLAUDE.md` files are thin Claude adapters that import their adjacent `AGENTS.md`.
* `.github/copilot-instructions.md` is the thin Copilot adapter. Copilot also discovers nested
  `AGENTS.md` files directly.
* `.claude/skills/` is the shared Agent Skills directory for Claude and Copilot. Do not mirror these
  skills into `.github/skills/` or `.github/prompts/`.
* Run `./gradlew validateAgentContext` after changing context files, skills, adapters, or the Gradle
  module graph. `.github/workflows/agent-context.yml` runs the same check for relevant changes.

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

Implementation files and applied plugins are documented in [`build-logic/AGENTS.md`](build-logic/AGENTS.md).

| Plugin ID | Purpose |
|-----------|---------|
| `apkanalyzer.agent-context` | Root task that validates shared AI context |
| `apkanalyzer.library` | Shared Android library, Kotlin, SDK, and formatting configuration |
| `apkanalyzer.application` | Android application, Firebase, and release configuration |
| `apkanalyzer.feature.api` | Feature navigation API modules |
| `apkanalyzer.feature.impl` | Feature implementation modules with Hilt, Compose, UI library, and navigation |
| `apkanalyzer.hilt` | Hilt and KSP |
| `apkanalyzer.compose` | Compose, Navigation 3, and serialization |
| `apkanalyzer.spotless` | Ktlint and Compose formatting rules |

## Key Dependencies (from libs.versions.toml)

[`gradle/libs.versions.toml`](gradle/libs.versions.toml) is the only source of dependency coordinates
and versions.

| Category | Libraries |
|----------|-----------|
| Compose | BOM, foundation, Material 3, material icons |
| Navigation | Navigation 3 runtime/UI, lifecycle integration, Hilt integration |
| Dependency injection | Hilt |
| Firebase | Analytics, Crashlytics, Performance |
| Lifecycle | Runtime, ViewModel, Compose, process lifecycle |
| Kotlin | Immutable collections, serialization, coroutines |
| Images | Coil Compose |
| Logging | Timber behind `core:common`'s `Logger` |
| Debugging | LeakCanary |
| Formatting | Spotless, ktlint, compose-rules-ktlint |

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

These files follow the Agent Skills standard and are discovered directly by both Claude and Copilot.
**You MUST read the relevant skill file before performing any of these tasks.** Skills contain
step-by-step instructions, templates, and checklists. Agents may load them automatically; hosts that
expose skill commands use the directory name as the slash command.

| Skill | Availability | When to Use |
|-------|--------------|-------------|
| [`create-feature-module`](.claude/skills/create-feature-module/SKILL.md) | Claude, Copilot | Creating a feature api/impl pair, NavKey, entry provider, and wiring |
| [`create-core-module`](.claude/skills/create-core-module/SKILL.md) | Claude, Copilot | Creating a shared repository, manager, utility, or data layer |
| [`create-compose-component`](.claude/skills/create-compose-component/SKILL.md) | Claude, Copilot | Adding a reusable component to `:core:ui-library` |
| [`implement-navigation`](.claude/skills/implement-navigation/SKILL.md) | Claude, Copilot | Adding destinations, entry providers, or navigation flows |
| [`spotless-fix`](.claude/skills/spotless-fix/SKILL.md) | Claude, Copilot | Formatting Kotlin or fixing ktlint violations |
| [`git-commit-author`](.claude/skills/git-commit-author/SKILL.md) | Claude, Copilot | Creating any commit |
| [`setup-local-tools`](.claude/skills/setup-local-tools/SKILL.md) | Claude, Copilot | Checking or setting up required development tools |
| [`analyze-ci-failure`](.claude/skills/analyze-ci-failure/SKILL.md) | Claude, Copilot | Inspecting GitHub Actions status or diagnosing a failed run |
| [`run-app`](.claude/skills/run-app/SKILL.md) | Claude, Copilot | Building, installing, and launching on a device or emulator |
| [`sync-design-changes`](.claude/skills/sync-design-changes/SKILL.md) | Claude with `DesignSync` | Applying changes from the Claude Design project |

## Production References

Use production code instead of copying templates into new files:

* [Assisted-injected ViewModel with `MutableStateFlow`](feature/app-detail/impl/src/main/kotlin/sk/styk/martin/apkanalyzer/feature/appdetail/impl/AppDetailViewModel.kt)
* [Standard flow-backed ViewModel with state, events, and actions](feature/settings/impl/src/main/kotlin/sk/styk/martin/apkanalyzer/feature/settings/impl/SettingsViewModel.kt)
