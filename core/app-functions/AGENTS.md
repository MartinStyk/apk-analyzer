# core:app-functions Module

## Purpose and Boundary

Exposes read-only app-analysis lookups as Android [App Functions](https://developer.android.com/ai/appfunctions)
so an on-device AI agent (Gemini or another assistant) can search/filter installed apps and inspect
one app's version, SDK targeting, install source, permission grants, and signing certificates
without opening Apk Analyzer. The package is `sk.styk.martin.apkanalyzer.core.appfunctions`.

This module owns no analysis logic of its own. Every function is a thin, agent-shaped wrapper over
`core:apps`'s `AppDetailRepository` and `InstalledAppsRepository`. `InstalledApp` already carries
permission, install-source, target-SDK, size, and last-used data per app, so `findApps` filters and
sorts in one in-memory pass over `InstalledAppsRepository.apps()` rather than depending on
`core:app-index`'s device-wide reverse index — that index exists for `feature:browse`'s bucketed
counts, a different shape of question than "does this one list of apps match all these conditions."
Do not duplicate extraction here — add it to `core:apps` and call it from here.

## Structure

* `ApkAnalyzerAppFunctionService` — the single `@AppFunctionServiceEntryPoint` abstract class.
  Every `@AppFunction` is declared directly on this class per the framework's requirement, but each
  one is a one-line delegation to an injected use case in `usecase/` — the framework requires the
  annotation and KDoc to sit on this class's methods, not that the logic does.
* `usecase/` — one `internal class ...UseCase @Inject constructor(...)` per `@AppFunction`, each a
  `suspend operator fun invoke(...)`. `FindAppsUseCase` holds the combined filter/sort/validate
  logic; `GetAppDetailUseCase`, `GetAppCertificatesUseCase`, and `GetAppPermissionsUseCase` each
  wrap one `AppDetailRepository.details()` call and a mapping function. `AppFunctionErrors.kt` holds
  the one error shared by the three `getXxx` use cases. This still follows the framework's own
  guidance to call existing repositories directly rather than adding an abstraction layer around the
  OS service — a use case *is* that direct call, just factored out of the framework-bound class for
  readability and per-function separation now that the service has four functions.
* `model/` — the `@AppFunctionSerializable` response types (`AppSummary`, `AppDetailResult`,
  `CertificateSummary`, `PermissionGrantSummary`) returned to the agent. These are deliberately
  smaller and differently shaped than `core:apps`'s domain models (`InstalledApp`, `AppDetail`,
  `Certificate`, `UsedPermission`); map between them in each use case's private mapping function
  rather than exposing domain models directly.
* `AppSourceAgentLabel.kt` — the one `AppSource -> String` label mapping shared by `FindAppsUseCase`
  and `GetAppDetailUseCase`. Plain English text for the agent, not `stringResource` — this module has
  no UI and no `res/`.

KSP generates a concrete `ApkAnalyzerAppFunctionServiceImpl` and the XML schema at compile time from
the abstract `ApkAnalyzerAppFunctionService`; there is no hand-written service to register beyond
the manifest entry, which references the generated `...Impl` name (see
[`app/AGENTS.md`](../../app/AGENTS.md)). The two names matter for different things: the manifest
`<service>` and Hilt both need the generated `...Impl` class, but `adb shell cmd app_function`'s
`--function` identifiers use the *abstract* class instead —
`sk.styk.martin.apkanalyzer.core.appfunctions.ApkAnalyzerAppFunctionService#findApps`, not the
`...Impl` name. The generated schema reaches the final APK through Kotlin's ordinary
resources→java-resources packaging path (`assets/apk_analyzer_app_function_service.xml` as a
classpath resource), not through AGP's `res/`-driven asset merging — so `androidResources = false`
here is correct and does not drop it; verify with `unzip -l app-debug.apk | grep app_function`
rather than looking under `intermediates/assets/`, which stays empty.

## The KDoc Exception

The root `AGENTS.md` non-negotiable "never write comments or KDoc" has exactly one carve-out, scoped
to this module: KDoc on `@AppFunction` and `@AppFunctionSerializable` declarations (functions,
parameters, and serializable properties) is how `isDescribedByKDoc = true` tells the agent what a
function does and how to use it — the KDoc *is* the tool description the platform indexes, not
incidental documentation. Follow the agent-facing style already used throughout this module: an
imperative-verb summary, a "Required workflow: call X first..." line where one function's output
feeds another's input, `@param` validation notes, and `@throws` lines that tell the agent what to do
when a call fails. Do not add KDoc anywhere else in this module (private helpers, the Hilt-injected
fields, mapping functions) — the exception is for agent-indexed declarations only.

Every `@AppFunctionSerializable` class needs inline KDoc on each property; KSP does not read
class-level `@param`/`@property` tags for these classes.

An optional `@AppFunction` parameter must be a nullable type with a `null` default — KSP fails the
build (`Type kotlin.String cannot be optional`) on a non-nullable type with a non-null default like
`sortBy: String = "Name"`. Use `sortBy: String? = null` and resolve the default inside the use case
instead, the same way every other optional filter on `findApps` does.

## Compatibility

The module's own `minSdk` comes from the shared `MIN_SDK` (28) like every other module, but App
Functions is an Android 16 (API 36) platform capability. `ApkAnalyzerAppFunctionService` and its
manifest `<service>` entry (declared in `app`, see [`app/AGENTS.md`](../../app/AGENTS.md)) are
`@RequiresApi(36)` / `tools:targetApi="36"`. This is safe on older devices without a runtime check:
the service is a passive component nothing on API < 36 knows how to bind to (there is no
`android.app.appfunctions.AppFunctionService` action or `BIND_APP_FUNCTION_SERVICE` permission on
those platforms), so it never gets instantiated. Do not add defensive SDK-version checks inside the
class itself.

## Testing

App Functions cannot be exercised through the normal UI — verify with `adb shell cmd app_function`
(list, execute, set-enabled) against an API 36+ device or emulator. See the `appfunctions` skill for
the exact commands, KDoc-optimization guidance, and candidate-discovery workflow used to build this
module. Verified end to end on a Pixel 6 / API 37 emulator: all four functions are indexed and
listed, `execute-app-function` returns real data for each, and both error paths (blank `findApps`,
an unknown `packageName`) surface our custom `AppFunctionException` messages correctly.

`@AppFunctionStringValueConstraint` is platform-enforced, not documentation-only: passing a value
outside `installSource`'s or `sortBy`'s `enumValues` never reaches `FindAppsUseCase` — the platform
rejects it first with its own generic type-mismatch message, before our friendlier
`AppFunctionInvalidArgumentException` text ever gets a chance to run. Keep the manual
`AppSource.valueOf()` / `sortBy` validation anyway as defense-in-depth (it's what actually fires for
Gemini-originated calls or any future caller that doesn't go through this exact constraint path),
but don't expect its message to be what a caller sees for an out-of-enum string.
