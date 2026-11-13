# Guardrails

Build-failing checks that keep code in line with [`AGENTS.md`](../../AGENTS.md). There are no
warnings and no baselines: a check holds for the whole codebase, or it is not enabled.

A check exists only when the convention drifts in practice and nothing already catches it. Before
adding one, check the compiler and classpath, ktlint and compose-rules (Spotless), built-in detekt
rules, and Android lint. Prefer a built-in rule over a custom one, and a custom detekt rule over a
Gradle task.

## Custom detekt rules

The `apkanalyzer` rule set in [`build-logic/detekt-rules`](../../build-logic/detekt-rules/AGENTS.md).
Path scopes live in [`.detekt/detekt.yml`](../../.detekt/detekt.yml). `detektDebug` also analyzes
unit-test and test-fixture sources.

| Rule | Enforces |
|---|---|
| `NoComments` | No comments or KDoc, except KDoc on App Functions declarations |
| `AppFunctionKDocRequired` | `@AppFunctionDeclaration` and `@AppFunctionSerializable` have KDoc, the tool description agents read (the compiler accepts an empty one) |
| `NoKtlintSuppression` | No `@Suppress("ktlint:...")` |
| `ViewModelContract` | `@HiltViewModel`; extends `ViewModel()`; public API is only `state`, `events`, and `onAction`; events go through `Channel(BUFFERED)` exposed with `receiveAsFlow()`; no `Navigator` |
| `ContractTypeShape` | Action and Event are sealed interfaces; State is a sealed interface or an `@Immutable` data class without lambdas or mutable collections |
| `MutableFlowExposure` | No public mutable flow or channel; an explicit backing field instead of a private flow plus `asStateFlow()` |
| `EntryBlockDelegatesToScreen` | An `entry { }` block calls exactly one `*Screen` |
| `NavKeyShape` | NavKeys are `@Serializable` `data class` or `data object` |
| `ApiModuleContents` | Feature `api` modules hold only NavKeys |
| `ImplVisibility` | `*Impl` classes are not public |
| `SingletonRepositoryBinding` | `@Binds` for a Repository or Manager is scoped `@Singleton` |
| `SealedMemberDataObject` | Objects implementing a sealed type are `data object`s |
| `PreviewPresence`, `PreviewShape` | Files with non-private composables have previews; previews end in `Preview` and wrap the app theme |
| `NoRawColor` | No raw colors outside the design system |
| `HardcodedComposeString` | No string literals as user-facing text in Composables |

## Built-in checks

| Check | Enforces |
|---|---|
| detekt `ForbiddenImport` | No `android.util.Log`, `runBlocking`, `Executor`s, or `collectAsState` |
| detekt `ForbiddenMethodCall` | No `print`/`println`, `Thread`, `thread { }`, or `Thread.sleep` |
| detekt `ForbiddenSuppress` | No `@Suppress` of a guardrail |
| detekt `UnusedImport` | No unused imports (ktlint doesn't report them) |
| detekt `InjectDispatcher`, `GlobalCoroutineUsage`, `SuspendFunSwallowedCancellation` | Injected dispatchers, structured scopes, no swallowed cancellation |
| detekt `ElseCaseInsteadOfExhaustiveWhen` | A `when` over a sealed type or enum lists every case, so a new variant fails to compile until handled |
| detekt `DataClassShouldBeImmutable`, `Deprecation`, `MissingSuperCall` | No `var` in data classes, no deprecated APIs, no skipped `super` calls |
| detekt default rule set plus the extra style rules in `detekt.yml` | Everything at warning severity fails the build |
| ktlint and compose-rules (Spotless) | Present-tense callback names, private previews, no mutable Composable parameters, no wildcard imports |
| Android lint | Every warning fails the build (`warningsAsErrors`), including missing translations; only the dependency-version checks and `IconMissingDensityFolder` are off |

## validateArchitecture

[`ValidateArchitectureTask`](../../build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/ValidateArchitectureTask.kt)
reads module build files as text. It covers what detekt can't see: the module graph and build files.

| Rule | Check |
|---|---|
| A1 | `feature/*/api` declares no project dependency. |
| A2 | `core/**` never depends on a feature module. |
| A3 | A feature module reaches another feature only through its `api`. |
| A4 | `feature/*/domain` depends only on its own `api` and on `core`. |
| A5 | TV modules never depend on phone modules or `core:design-system:phone`. |
| A6 | Phone modules never depend on TV modules. |
| B1 | Module build files never set SDK levels, `jvmToolchain`, `jvmTarget`, or `JavaVersion`. |
| B2 | No `project(":...")`; typesafe accessors only. |
| B3 | Plugins applied with `alias(libs.plugins.*)` only. |
| B4 | No string-literal dependency coordinates. |
| B5 | Material3 (`core:design-system:phone`, `app`), TV Material (`core:design-system:tv`), and Material icons (`core:design-system:foundation`) are dependencies of those modules only. The compiler then rejects their imports everywhere else. |
| S1 | `src/test` and `src/testFixtures` only in `core/**` and `feature/*/domain`. |
| S2 | A declared `namespace` matches the module path with hyphens removed. |

## Not checked

Two indicators sharing one predicate, unique `LazyColumn` keys, logic in classes rather than
`object`s, repository methods that never throw, and user-facing copy. These stay with review.
