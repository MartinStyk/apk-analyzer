# Guardrails

Hard, build-failing checks that keep agent-written code in line with the conventions in
[`AGENTS.md`](../../AGENTS.md). Every check here fails CI. Nothing is a warning, and there are no
baselines. A check either holds for the whole codebase or is not enabled.

## Mechanisms

| Mechanism | Where | Run locally |
|---|---|---|
| Custom detekt rule set `apkanalyzer` | [`build-logic/detekt-rules`](../../build-logic/detekt-rules) | `./gradlew :core:apps:detektDebug` (any module) |
| Built-in detekt rules | [`.detekt/detekt.yml`](../../.detekt/detekt.yml) `style:` section | same as above |
| `validateArchitecture` | `build-logic/convention` `ValidateArchitectureTask` | `./gradlew validateArchitecture` |
| `validateTranslations` | `build-logic/convention` `ValidateTranslationsTask` | `./gradlew validateTranslations` |
| Rule unit tests | `build-logic/detekt-rules/src/test` | `./gradlew -p build-logic :detekt-rules:test` |

Rule scopes (which paths a rule covers) live in `.detekt/detekt.yml` under `apkanalyzer:` as
`includes`/`excludes`. To drop a rule, set `active: false` there. To narrow one, add an exclude.
Suppressing a guardrail with `@Suppress` is blocked by `ForbiddenSuppress`: fix the code, or change
the rule and this document.

The custom rules also run on `build-logic` itself (`:build-logic:convention:detektMain` and
`:build-logic:detekt-rules:detektMain`).

Known narrow exceptions:

* `PreviewPresence` skips `theme/`, `navigation/`, the app hosts, `core:design-system:foundation`
  (no theme wrapper there), and `MinimalScroll.kt` (a CompositionLocal provider with nothing to
  render).
* `HardcodedComposeString` skips preview code and `preview*`/`sample*` data, and ignores the debug
  `label` of `Crossfade`, `AnimatedContent`, and `AnimatedVisibility`.

## How This List Was Built

Three independent research passes looked at the written conventions, at what agent-written commits
actually got wrong in this repository's history, and at existing community rule sets (compose-rules,
appKODE, Konsist, ArchUnit, dependency-analysis, modules-graph-assert). A separate scorer rated each
candidate on drift likelihood (D), precision (P), conformity cost (C), and implementation cost (I),
each 1 to 5. Third-party rule sets were used for inspiration only, because adding dependencies needs
approval.

## Catalogue

Status is **Enabled**, **Covered** (enforced by another entry), **Deferred** (reasonable, not done
yet), or **Rejected**. To add a deferred entry or drop an enabled one, edit this table along with the
rule.

| ID | Guardrail | Status | Mechanism |
|---|---|---|---|
| G-01 | No comments or KDoc outside the App Functions carve-out | Enabled | `apkanalyzer:NoComments` |
| G-02 | KDoc required on `@AppFunctionDeclaration` and `@AppFunctionSerializable` | Enabled | `apkanalyzer:AppFunctionKDocRequired` |
| G-03 | `androidx.compose.material3` only in `app` and `core/design-system/phone` | Enabled | `apkanalyzer:Material3Scope` |
| G-04 | `androidx.tv.material3` only in `core/design-system/tv` and `tv` | Enabled | `apkanalyzer:TvMaterial3Scope` |
| G-05 | `androidx.compose.material.icons` only in the design system | Enabled | `apkanalyzer:MaterialIconsScope` |
| G-06 | Forbidden imports: Timber, `android.util.Log`, `Dispatchers`, Koin, Dagger components, legacy navigation, view inflation, executors, Material2, foreign test assertion libraries | Enabled | detekt `ForbiddenImport` |
| G-07 | No `runBlocking`, threads, `Thread.sleep`, `println`, `printStackTrace`, `GlobalScope` | Enabled | `apkanalyzer:BlockingOrConsoleCall` |
| G-08 | No `@Suppress` of a guardrail | Enabled | detekt `ForbiddenSuppress` |
| G-09 | No lint or detekt baseline files | Enabled | `validateArchitecture` B5 |
| G-10 | No unused imports | Enabled | detekt `UnusedImport` |
| G-11 | ViewModel contract: extends `ViewModel()`, one public `state`, one public `onAction`, optional `events` | Enabled | `apkanalyzer:ViewModelContract` |
| G-12 | ViewModels never touch `Navigator` | Enabled | `apkanalyzer:ViewModelContract` |
| G-13 | ViewModel classes are not public | Enabled | `apkanalyzer:ViewModelContract` |
| G-14 | No `_x`/`asStateFlow()` pairs, no exposed mutable flows or channels | Enabled | `apkanalyzer:MutableFlowExposure` |
| G-15 | Events through `Channel(Channel.BUFFERED)`, never `MutableSharedFlow` | Enabled | `apkanalyzer:ViewModelContract` |
| G-16 | Action and Event are sealed interfaces; State is a sealed interface or `@Immutable` data class without lambdas | Enabled | `apkanalyzer:ContractTypeShape` |
| G-17 | Objects in sealed hierarchies are `data object` | Enabled | `apkanalyzer:SealedMemberDataObject` |
| G-18 | NavKeys are `@Serializable` `data class` or `data object` | Enabled | `apkanalyzer:NavKeyShape` |
| G-19 | `entry { }` blocks only delegate to one `*Screen` | Enabled | `apkanalyzer:EntryBlockDelegatesToScreen` |
| G-20 | Feature `api` modules contain only NavKeys | Enabled | `apkanalyzer:ApiModuleContents` |
| G-21 | `TAG` is a file-level `private const val` | Enabled | `apkanalyzer:LoggerTag` |
| G-22 | `*Impl` classes are `internal` or `private` | Enabled | `apkanalyzer:ImplVisibility` |
| G-23 | `*Impl` and its interface live in separate files | Enabled | `apkanalyzer:ImplSeparateFile` |
| G-24 | Hilt module shape: `@InstallIn`, `@Binds` in interfaces, `@Provides` in classes | Enabled | `apkanalyzer:HiltModuleShape` |
| G-25 | No hardcoded dispatchers | Covered | G-06 |
| G-26 | `runCatchingCancellable` instead of `runCatching` in suspending code | Enabled | `apkanalyzer:RunCatchingInSuspend` |
| G-27 | Present-tense Composable callbacks (`onClick`, not `onClicked`) | Enabled | `apkanalyzer:PresentTenseCallbacks` |
| G-28 | Files with UI Composables have previews; previews are private, end in `Preview`, wrap the app theme | Enabled | `apkanalyzer:PreviewPresence`, `apkanalyzer:PreviewShape` |
| G-29 | No ktlint suppression in code | Enabled | `apkanalyzer:NoKtlintSuppression` |
| G-30 | Module graph rules A1 to A7 | Enabled | `validateArchitecture` |
| G-31 | Build file hygiene B1 to B4 | Enabled | `validateArchitecture` |
| G-32 | Structure rules S1 to S4 | Enabled | `validateArchitecture` |
| G-33 | Package matches directory | Enabled | detekt `InvalidPackageDeclaration` |
| G-34 | No `TODO`, `FIXME`, `HACK`, or `XXX` markers | Covered | G-01 |
| G-35 | No `MaterialTheme` outside the design system | Covered | G-03, G-04 |
| G-36 | No raw colors outside the design system | Enabled | `apkanalyzer:NoRawColor` |
| G-37 | No hardcoded user-facing strings in Composables | Enabled | `apkanalyzer:HardcodedComposeString` |
| G-38 | Tests use JUnit 4 and Kotest assertions only | Enabled | detekt `ForbiddenImport` |
| G-39 | Every locale has exactly the translatable base keys, with matching placeholders | Enabled | `validateTranslations` |
| G-40 | No mutable collections in State or Composable parameters | Enabled | `apkanalyzer:ContractTypeShape` |
| G-41 | No wildcard imports | Enabled | detekt `WildcardImport` |
| G-42 | No `Boolean?` tri-state properties | Deferred | Too many legitimate uses; revisit for public model properties only |
| G-43 | Repository and manager methods never throw | Rejected | Needs flow analysis; helpers throw inside `runCatching` legitimately |
| G-44 | Logic lives in classes, not objects | Rejected | Cannot tell constant holders from logic without semantics |
| G-45 | Two indicators of one fact share a predicate | Rejected | Semantic, review only |
| G-46 | Extract a shared helper on the second consumer | Rejected | Duplicate detection is too noisy for Compose |
| G-47 | `LazyColumn` keys unique across the list | Rejected | Runtime property |
| G-48 | No identifiers or API names in user-facing copy | Deferred | Possible `validateTranslations` extension |
| G-49 | Kotlin explicit API mode | Rejected | Large churn; G-13 and G-22 cover the real drift |
| G-50 | Extra built-in style rules such as `UseDataClass` and nesting depth | Rejected | Style noise, not drift |
| G-51 | No `else ->` in a `when` over a sealed type | Deferred | Zero hits; built-in `ElseCaseInsteadOfExhaustiveWhen` can be enabled later |
| G-52 | Lint `warningsAsErrors` | Deferred | Needs a dedicated cleanup pass |
| G-53 | New dependencies need approval | Deferred | Governance, for example `CODEOWNERS` on `gradle/libs.versions.toml` |
| G-54 | Third-party rule sets | Rejected | New dependencies; used as inspiration |
| G-55 | `collectAsStateWithLifecycle`, never `collectAsState` | Enabled | detekt `ForbiddenImport` |
| G-56 | `hiltViewModel()` only as a screen default parameter | Covered | compose-rules `ViewModelInjection` via ktlint |
| G-57 | No `MaterialTheme.colorScheme` or `Icons` outside the design system | Covered | G-03, G-05 |
| G-58 | `@Immutable` on State data classes | Enabled | `apkanalyzer:ContractTypeShape` (the `@Stable` half is rejected: needs type resolution) |
| G-59 | TV never depends on phone modules or Material3 | Enabled | `validateArchitecture` A5 |
| G-60 | Skills and templates produce conforming code | Enabled | Documentation fix in the scaffolding skills |

## validateArchitecture Rules

| Rule | Check |
|---|---|
| A1 | `feature/*/api` declares no project dependency. |
| A2 | `core/**` never depends on a feature module. |
| A3 | A feature module reaches another feature only through its `api`. |
| A4 | `feature/*/domain` never depends on a `phone` or `tv` module. |
| A5 | TV modules never depend on phone modules, `core:design-system:phone`, or phone Material3. |
| A6 | Phone modules never depend on TV modules. |
| A7 | Only `app` depends on feature `phone` modules, and only `tv` on feature `tv` modules. |
| B1 | Module build files never set SDK levels, `jvmToolchain`, `jvmTarget`, or `JavaVersion`. |
| B2 | No `project(":...")`; typesafe accessors only. |
| B3 | Plugins applied with `alias(libs.plugins.*)` only. |
| B4 | No string-literal dependency coordinates. |
| B5 | No `*baseline*.xml` file anywhere. Baseline profiles (`baseline-prof.txt`) are fine. |
| S1 | No `res/layout*` directories. |
| S2 | `src/test` and `src/testFixtures` only in `core/**` and `feature/*/domain`. |
| S3 | No `src/androidTest`. |
| S4 | A declared `namespace` matches the module path with hyphens removed. |
