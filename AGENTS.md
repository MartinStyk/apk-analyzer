# Apk Analyzer Agent Instructions

Android app for inspecting installed apps and APK files — permissions, components, certificates,
device-wide statistics. Multi-module, Kotlin, Compose, Hilt.

**Every module has its own `AGENTS.md`. Read it before working inside that module** instead of
re-deriving the module's structure.

## Non-Negotiables

* Kotlin only. Jetpack Compose only — no XML layouts. Hilt only — no Dagger or Koin.
* Coroutines and flows only for concurrency. Never `Thread`, `Executor`, or `runBlocking`.
* Don't add a dependency. [`gradle/libs.versions.toml`](gradle/libs.versions.toml) is the only
  source of coordinates and versions; if a new library seems necessary, ask first.
* Never write comments or KDoc — not even when the WHY seems non-obvious. Use self-documenting
  names and structure. The only exception is an explicit request for a comment in that instance.
* Never hardcode a user-facing string in a Composable. Use `stringResource` backed by
  `res/values/strings.xml` in the module that owns the UI.
* Never hardcode SDK levels or the JVM toolchain in a module's `build.gradle.kts` — they come from
  [`AndroidSdk.kt`](build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/utils/AndroidSdk.kt)
  and [`Kotlin.kt`](build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/utils/Kotlin.kt).
* **Commits are authored as the human user only.** Never add `Co-Authored-By: Claude`,
  `Claude-Session:`, or any AI co-author trailer. See the `git-commit-author` skill.

## Development Workflow

Before changing code:

1. Understand the requirement and define its acceptance criteria.
2. Search the repository for existing implementations of similar behavior.
3. Identify the relevant modules, classes, interfaces, and module-level `AGENTS.md` files.
4. Understand the existing architecture, patterns, and constraints.
5. Create a concise implementation plan.
6. Implement the smallest clean solution that satisfies the requirement and fits the architecture.
7. Run the relevant build checks from [Verifying a Change](#verifying-a-change).
8. Inspect the complete Git diff, including changes made earlier in the worktree.
9. Perform a self-review for correctness, scope, architecture, failure states, and leftover code.
10. Fix every issue found during self-review.
11. Run the relevant verification again after the final fix.

Do not start implementing before understanding the existing implementation and patterns. Do not
introduce a new abstraction, framework, architecture pattern, or utility when an existing solution
can be reused.

## Engineering Principles

* Prefer existing patterns over introducing new ones.
* Keep changes focused on the requested behavior and do not modify unrelated files.
* Prefer simple solutions over clever ones and avoid speculative abstractions.
* Reuse existing functionality instead of duplicating it.
* Preserve the architecture. Move or restructure code when necessary for a clean implementation,
  but ask before making a large architectural change.
* Consider failure states, empty states, and cancellation paths.
* Do not hide errors with broad exception handling.
* Do not leave debug code, temporary logging, or TODOs in production code.

## Verifying a Change

Move fast: don't run `detektDebug`, `lintDebug`, `spotlessCheck`, or `validateAgentContext` after
every edit. CI runs the whole-app check on every push — let it catch those, and fix failures in a
follow-up commit instead of gating local iteration on them.

| When | Run |
|---|---|
| Iterating on one module | `./gradlew :feature:apps:impl:compileDebugKotlin` |
| Before committing | `./gradlew spotlessApply` — the only gate required locally |
| Whole-app check (what CI gates on — don't run this locally unless debugging a CI failure) | `./gradlew spotlessCheck detektDebug :build-logic:convention:detektMain lintDebug :app:assembleDebug` |
| After changing a context file, skill, adapter, or the module graph | `./gradlew validateAgentContext` — skip this too; CI covers it |

A successful compile is not proof a Compose layout is correct. For visual or layout changes, use
the `run-app` skill and look at it on a device. Two failure modes that pass every gate: text that
wraps to three lines inside a fixed-width column, and two indicators that disagree because each
derived its own answer.

`spotlessCheck` does not flag unused imports. Deleting the last use of something does not delete its
import, and no gate will tell you — check by hand.

## Module Rules

Modules live under `app/`, `core/<name>/`, and `feature/<name>/{api,impl}/`; `settings.gradle.kts`
is the authoritative list.

* `feature/*/api` — depends on nothing, holds only `@Serializable` NavKeys and the tab-label string.
* `feature/*/impl` — `api(projects.feature.<name>.api)` plus whichever `core` modules it needs.
* `core/*` — may depend on other `core` modules. **Never** depends on a `feature` module.
* A feature never depends on another feature's `impl`. Cross-feature navigation goes through the
  target's `api` module.
* `app` — wiring only: the launcher Activity, the external-APK document Activity, nav hosts, and
  app-scoped Hilt bindings. Put no feature logic here.
* Declare dependencies with typesafe accessors (`projects.core.appPermissions`), never
  `project(":core:app-permissions")`. Apply plugins with `alias(libs.plugins.apkanalyzer.*)`.
* A module's package matches its directory with hyphens removed: `core/user-preferences` →
  `core.userpreferences`, `feature/app-detail/impl` → `feature.appdetail.impl`. `app` uses the root
  package `sk.styk.martin.apkanalyzer` with no suffix.
## Architecture

### ViewModels

* Extend `ViewModel()` directly. No base class.
* Expose exactly one `val state: StateFlow<FeatureState>` and one
  `fun onAction(action: FeatureAction)` with a `when` dispatch. No other public methods.
* One-shot events go through `Channel<Event>(Channel.BUFFERED)` exposed as `receiveAsFlow()` —
  never as state.
* **State** — `sealed interface` or `@Immutable data class`. Never holds lambdas.
* **Event** — `sealed interface`. ViewModel→UI signals (navigation, toasts, system intents).
* **Action** — `sealed interface`. UI→ViewModel intents.
* Model back navigation as both an Action and an Event so the ViewModel never touches `Navigator`.
* In Composables: `collectAsStateWithLifecycle()` for state, `LaunchedEffect` for events.

### Data Layer

* Repositories and Managers are a public `interface` plus an `internal` `Impl` in the same module,
  bound with Hilt `@Binds` and scoped `@Singleton`. Keep the interface and implementation in
  separate files. Models declared alongside the interface stay in the interface file.
* Interface methods never throw. Return `Result<T>`, a nullable `T?`, or an empty collection.
* Inject `DispatcherProvider` and switch with `flowOn(dispatcherProvider.default())` or
  `withContext(dispatcherProvider.io())`. Never hardcode `Dispatchers.IO`.

### Hilt

* `@HiltViewModel` + `@Inject constructor`. When a ViewModel needs a runtime parameter, use
  `@HiltViewModel(assistedFactory = VM.Factory::class)` + `@AssistedFactory` + `@AssistedInject`.
* Modules are `@Module @InstallIn(SingletonComponent::class)` — an `interface` with `@Binds` for
  interfaces, a `class` with `@Provides` only for platform types.
* Prefer constructor injection over module `@Provides`.

### Navigation

* **Navigation 3** only. No legacy Jetpack Navigation.
* NavKeys are `@Serializable` and implement `NavKey`. `data class` when carrying parameters,
  `data object` when not. Keys reachable from other features go in `feature/*/api`; keys internal
  to one feature go in that feature's `impl/navigation/`.
* Register screens via `EntryProviderScope<NavKey>.<feature>Entries(navigator: Navigator)` in
  `feature/*/impl/navigation/`, then wire that call into the `entryProvider { }` block in
  `app/src/main/kotlin/sk/styk/martin/apkanalyzer/ui/ApkAnalyzerApp.kt`. A screen that isn't wired
  there is unreachable.
* Transitions: `bottomEntryMetadata()` / `slideFromEndEntryMetadata()` from
  `sk.styk.martin.apkanalyzer.core.uilibrary.animation`.
* Multi-stack state lives in `:core:navigation` — see [`core/navigation/AGENTS.md`](core/navigation/AGENTS.md).

## Compose

* **Feature modules must never import `androidx.compose.material3`.** Use Compose foundation APIs
  plus the wrappers in `:core:ui-library`. If a component isn't wrapped yet, wrap it there first —
  see the `create-compose-component` skill. (`app` uses material3 directly for `Scaffold` and theme
  plumbing; that is the only exception.)
* Colors and type come from `AppTheme.colors` / `AppTheme.typography`, icons from
  `ApkAnalyzerIcons`. Never hardcode a color or reach for `MaterialTheme.colorScheme` outside
  `:core:ui-library`.
* Check the component inventory in [`core/ui-library/AGENTS.md`](core/ui-library/AGENTS.md) before
  calling a component — names don't always match file names.
* Every list property in State classes and Composable parameters is an `ImmutableList` from
  `kotlinx.collections.immutable`. `@Immutable` on State data classes; `@Stable` on non-data
  classes used as Composable parameters.
* Every file with `@Composable` functions has `@Preview` functions: `private`, suffixed `Preview`,
  wrapped in `ApkAnalyzerTheme { }`, with realistic sample data. Preview the stateless content
  composable, never the ViewModel-dependent screen.
* Composable callbacks are present tense — `onClick`, `onSelectItem`, `onBack`. Never
  `onClicked`, `onItemSelected`, `onBackPressed`.
* **`LazyColumn` item keys must be unique across the whole list, not per section.** A sectioned list
  keyed on an item identifier crashes when the same identifier lands in two sections. Deduplicate in
  the ViewModel rather than compounding the key with the section.
* **Two indicators of the same fact must come from one predicate.** A verdict line and a row of
  icons that each recompute "is this a problem" will eventually contradict each other on screen.
* **A row that has nothing to show on tap means the item sheet is missing, not that the idiom is
  optional.** Don't neutralise the tap (`indication = null`, a no-op `onClick`) to hide a dead
  affordance — every list row in app detail is `tap = explain, long-press = copy`.

## Conventions

* `data object`, not plain `object`, for sealed interface members.
* **A nullable primitive that encodes a variant or a third state is a type in disguise.** `Boolean?`
  meaning yes/no/unknown, or a `String?` whose null marks "a different kind of thing", belongs in an
  enum or a sealed interface. `Feature` (`Hardware` vs `OpenGlEs`) and `FeatureAvailability` in
  `core:apps` are the worked examples — both replaced exactly that shape.
* **Extract a shared helper when the second consumer appears — and grep for a third before writing
  your own.** Duplicated composables here have reached three identical copies before anyone noticed.
* **A core module packages by domain once it holds more than one repository/manager family, not by
  layer.** A flat root full of repositories, a flat `model/` holding every data class, and a flat
  `analysis`/`util` grab-bag for internals is the shape to avoid — it forces you to open three
  unrelated folders to understand one concept. Once a second repository/manager family shows up,
  give each family its own subpackage holding its interface, impl, models, and any private
  supporting types together; keep only the handful of models every family composes into at the
  module's `model/` root. `core:apps`'s `signing/`, `permissions/`, `components/`, `manifest/`,
  `installsource/`, `devicefeatures/`, `packaging/`, `usagestats/`, `storagestats/`, `export/`, and
  `sdkversion/` packages are the worked example. A module with exactly one repository/manager family
  (e.g. `core:app-permissions`, `core:app-index`, `core:apk-files`) has no reason to do this yet —
  don't pre-split a module that hasn't earned it.
* No wildcard imports.
* Prefer `private`; `internal` for module-visible; `public` only for actual public API.
* `Logger` from `core.common.logger`, never raw Timber: `Logger.d("Tag", "msg")`,
  `Logger.e("Tag", throwable, "msg")`. Declare `private const val TAG` at file level.
* `@Serializable` for nav keys and new models. Parcelize only for existing Android-specific data
  already passed through intents/bundles.
* No test infrastructure or test dependencies exist here. Don't add tests, test dependencies, or
  test source sets unless explicitly asked.

## User-Facing Copy

Write for non-technical users who understand Android basics. Active voice, present tense, sentence
case. Name the concrete thing rather than gesturing at it — "Shows which apps use the Camera
permission", not "View permission information". Never surface class names, internal identifiers, or
Android API names in a user-facing string.

## Skills

**Read the relevant skill in [`.claude/skills/`](.claude/skills/) before starting a matching
task** — skills hold the step-by-step procedures this file deliberately omits. Each skill's
frontmatter states when it applies.

`create-feature-module`, `create-core-module`, `create-compose-component`, `implement-navigation`,
`spotless-fix`, `git-commit-author`, `setup-local-tools`, `analyze-ci-failure`, `run-app`,
`navigate-app-adb`, `sync-design-changes`.

All are shared by Claude and Copilot except `sync-design-changes`, which needs Claude's `DesignSync`
tool.

## Shared AI Context

* `AGENTS.md` files are canonical. Put guidance in the closest relevant `AGENTS.md`; never copy it
  into a tool-specific file.
* Scoped `AGENTS.md` files document durable context that cannot be inferred cheaply: module
  boundaries, package-level organization, required patterns, non-obvious behavior, and exceptional
  entry points.
* Do not maintain exhaustive file trees, copied interfaces, ordinary dependency lists, or
  implementation summaries that repository search can recover more accurately. Name a specific
  file only when it is a canonical reference, an assembly point, or misleadingly named.
* Prefer package or domain maps over source-file inventories. Adding or renaming an ordinary source
  file should not require an `AGENTS.md` update.
* `CLAUDE.md` files contain exactly `@AGENTS.md` and nothing else.
* `.github/copilot-instructions.md` is the Copilot adapter; Copilot also reads nested `AGENTS.md`
  directly. `.claude/skills/` is shared by both tools — never mirror skills into `.github/skills/`,
  `.github/prompts/`, or `.agents/skills/`.
* `validateAgentContext` enforces AGENTS/CLAUDE pairing, per-module coverage, skill frontmatter,
  duplicate skill locations, and that every local markdown link resolves.

## Production References

Read these instead of copying a template into a new file:

* [Assisted-injected ViewModel with `MutableStateFlow`](feature/app-detail/impl/src/main/kotlin/sk/styk/martin/apkanalyzer/feature/appdetail/impl/AppDetailViewModel.kt)
* [Standard flow-backed ViewModel with state, events, and actions](feature/settings/impl/src/main/kotlin/sk/styk/martin/apkanalyzer/feature/settings/impl/SettingsViewModel.kt)
