# core:ai-insights Module

## Purpose and Boundary

Home for on-device AI features. Today it holds exactly one: a short, factual summary of one app
(installed package or APK file) generated from data already produced by `core:apps`. The package is
`sk.styk.martin.apkanalyzer.core.aiinsights`.

This module owns no UI. `feature/app-detail/impl`'s `components/aisummary/` package owns the
`AiSummaryCard` composable and its ViewModel, and they are the only consumers of
`AppAiDescriptionRepository`.

## Package Map

* `ai/` — the on-device model engine, shared by any future AI feature in this module.
  `OnDeviceAiEngine` is the only place that knows ML Kit exists; `OnDeviceAiEngineImpl` wraps the
  GenAI Prompt API (`com.google.mlkit:genai-prompt`) and hands back plain text or an `AiAvailability`
  status. `AiAvailability` (`Available` / `Downloadable` / `Downloading` / `Unavailable`) is public
  because it is part of `AppAiDescriptionRepository`'s public surface.
* `di/AiInsightsModule` — binds `OnDeviceAiEngine` and provides the `GenerativeModel` client.
  Constructor-inject `GenerativeModel` rather than instantiating it inline.
* `appdescription/` — the app-description feature: repository, prompt generation, caching, and
  metadata extraction. A second AI feature gets its own top-level package alongside this one, reusing
  `ai/` rather than duplicating engine plumbing.

## Public Surface

* `AppAiDescriptionRepository` (`appdescription/`):
  * `availability: StateFlow<AiAvailability>` — a single, repository-owned hot stream, not a per-call
    snapshot. Every `AiSummaryViewModel` collects it directly instead of polling; a `Downloadable`
    value means the UI should offer a download action, `Downloading` means a download — whether or
    not this screen started it — is already running. Because it's shared and hot, a screen opened
    mid-download sees `Downloading` immediately, and a screen already open when another screen starts
    a download updates live without needing to be recreated.
  * `downloadModel()` — fire-and-forget; starts a model download, or does nothing if one is already
    running (guarded by `@Synchronized` + `downloadJob`). The download runs in the repository's
    `appScope`, not the caller's `viewModelScope`, so navigating away from the screen that triggered
    it does **not** cancel it. Progress and outcome are observed through `availability`, not through
    this function's (lack of a) return value.
  * `getDescription(reference: AppReference): AppAiDescription?` — never throws; returns `null`
    whenever the feature cannot produce a valid description (AI not `Available`, generation failure,
    invalid output, metadata lookup failure). Does not check availability on the caller's behalf —
    only call it once `availability` reports `Available`.
* `AppAiDescription(description)` — the only public model. User-facing text only; no prompt, model
  name, package metadata, or cache state leaks through it.

Everything else in this module is `internal`.

## Data Source

This module never touches `PackageManager` directly. `appdescription/metadata/AppDetailMetadataProvider`
calls the existing `AppDetailRepository.details(reference)` (`core:apps`) and adapts its `AppDetail`
into the normalized `AppAiContext` the AI layer consumes — reusing `core:apps`'s extraction rather
than duplicating it, per that module's own boundary rule.

`AppAiContext` carries only `packageName`, `appName`, `targetSdk`, and `permissions` — activities,
services, and receivers were deliberately dropped from the prompt. Release builds are routinely
R8-obfuscated, so component class names carry near-zero signal for the model while still costing
prompt budget, and the validator already forbids echoing them back regardless. Permissions come from
`Permissions.used` (what the app requests, the semantic signal) and never from `Permissions.defined`
(what the app declares for other apps). They are ranked by protection level — `Dangerous`, then
`Normal`, `Signature`, `Internal`, then permissions with no resolvable details — and truncated to 30,
so the most meaningful ones survive. `AppAiContext`'s fields are deduplicated and deterministically
ordered, so reordered permissions cannot change the `inputHash` and cannot trigger a pointless
regeneration.

## Flow

`getDescription` → `AppDetailMetadataProvider` → normalize → `inputHash` → cache lookup (installed
packages only) → on miss, check AI availability → generate → validate → one same-prompt retry on
invalid output → cache and return, or return `null`. Concurrent calls for the same `AppReference` are
coalesced (`Mutex` + in-flight `Deferred` map in `AppAiDescriptionRepositoryImpl`) rather than
triggering duplicate generations.

`AiDescriptionGenerator.inputHash(context)` hashes the fully-built prompt
(`PromptBuilder.build(context)`), not a separately hand-maintained version number — any change to the
prompt template or output contract invalidates the cache on its own, with nothing to remember to bump.

`loadDescription` is wrapped in the `ai_summary_load` performance trace (`core:common`'s
`PerformanceTracker`/`PerformanceTrace`, see
[`docs/app/technical/repository-load-performance.md`](../../docs/app/technical/repository-load-performance.md)) —
one trace per coalesced generation, not per `getDescription` call, so concurrent callers for the same
`AppReference` share it. `outcome=degraded` covers every expected `null` result (AI unavailable,
metadata failure, no valid output after retry); `outcome=error` is reserved for a genuine unhandled
exception such as a cache I/O failure.

## AI Integration

`appdescription/generation/AiDescriptionGeneratorImpl` stays engine-agnostic — it composes
`PromptBuilder`, `OnDeviceAiEngine`, and `DescriptionParser` (markdown-fence-tolerant JSON). There is
no lenient/strict prompt variant: the prompt always instructs JSON-only output, and the one retry on
invalid output simply re-runs the same prompt. `DescriptionValidator` rejects model/implementation
leakage and any verbatim identifier from the input (package name, permission names), so
prompt-echoing output never reaches the UI. No cloud AI, no API key, ever.

## Cache

Room-backed, single table (`AppAiDescriptionEntity`, PK `packageName`) — exactly one row per package,
never a history of versions. The DAO filters on `packageName` *and* `inputHash` in SQL; nothing is
post-filtered in Kotlin. No separate index on `inputHash` is needed: `packageName` is already the
unique key, so a lookup touches at most one row.

Only `AppReference.InstalledPackage` results are cached. `AppReference.ApkFile` results are generated
fresh every time and never read from or written to the cache — the cache key is `packageName` alone,
so caching an arbitrary analyzed APK would collide with (and overwrite) the installed app's cached row
whenever their content differs. APK-file inspection is normally a one-off anyway.

This is this repository's first use of Room; `apkanalyzer.room` (`build-logic/convention`) is the
convention plugin other modules should reuse rather than reconfiguring Room per module.

## Privacy

Never log a full prompt or a full generated description — package names are fine (matching
`core:apps` conventions), the content sent to or received from the model is not.
