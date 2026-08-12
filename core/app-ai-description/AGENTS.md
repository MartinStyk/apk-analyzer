# core:app-ai-description Module

## Purpose and Boundary

Generates a short, factual, on-device AI summary of one app (installed package or APK file) from
data already produced by `core:apps`, caches it, and hides itself entirely when on-device AI is
unavailable or generation fails. The package is `sk.styk.martin.apkanalyzer.core.appaidescription`.

This module owns no UI. Issue #146's specification asked for the card to live here; that is
superseded — UI belongs to a feature module under this repository's module rules, so
`feature/app-detail/impl` owns the `AiSummaryCard` composable and its ViewModel, and they are the
only consumers of `AppAiDescriptionRepository`.

## Public Surface

* `AppAiDescriptionRepository.getDescription(reference: AppReference): AppAiDescription?` — the
  only public entry point. Never throws; returns `null` whenever the feature cannot produce a valid
  description (AI unavailable, generation failure, invalid output, metadata lookup failure).
* `AppAiDescription(shortDescription, longDescription)` — the only public model. Contains
  user-facing text only; no prompt, model name, package metadata, or cache state leaks through it.

Everything else in this module is `internal`.

## Data Source

This module never touches `PackageManager` directly. `metadata/AppDetailMetadataProvider` calls the
existing `AppDetailRepository.details(reference)` (`core:apps`) and adapts its `AppDetail` into the
normalized `AppAiContext` the AI layer consumes — reusing `core:apps`'s extraction rather than
duplicating it, per that module's own boundary rule. `AppAiContext` normalization caps the
serialized size at 8,000 characters and deduplicates/truncates before anything is sent to the model.

Permissions come from `Permissions.used` (what the app requests, the semantic signal) and never from
`Permissions.defined` (what the app declares for other apps). They are ranked by protection level —
`Dangerous`, then `Normal`, `Signature`, `Internal`, then permissions with no resolvable details —
and truncated, so the most meaningful ones survive the cap. Every list in `AppAiContext` is
deduplicated and deterministically ordered, so reordered permissions or components cannot change the
`inputHash` and cannot trigger a pointless regeneration.

## Flow

`getDescription` → `AppDetailMetadataProvider` → normalize → `inputHash` → cache lookup keyed on
`(packageName, inputHash)` → on miss, check AI availability → generate → validate →
one stricter-prompt retry on invalid output → cache and return, or return `null`. Concurrent calls
for the same `AppReference` are coalesced (`Mutex` + in-flight `Deferred` map in
`AppAiDescriptionRepositoryImpl`) rather than triggering duplicate generations.

## AI Integration

`ai/OnDeviceAiEngine` is the only place that knows ML Kit exists. `OnDeviceAiEngineImpl` wraps the
GenAI Prompt API (`com.google.mlkit:genai-prompt`): model availability, download of a `DOWNLOADABLE`
model, prompt execution, and failure swallowing all live there, and it hands back plain text.
`generation/AiDescriptionGeneratorImpl` stays engine-agnostic — it composes `PromptBuilder`,
the engine, and `DescriptionParser` (markdown-fence-tolerant JSON). `DescriptionValidator` rejects
model/implementation leakage and any verbatim identifier from the input (package name, permission
names, component names), so prompt-echoing output never reaches the UI. No cloud AI, no API key,
ever.

## Cache

Room-backed, single table (`AppAiDescriptionEntity`, PK `packageName`) — exactly one row per
package, never a history of versions. The DAO filters on `packageName` *and* `inputHash` in SQL;
nothing is post-filtered in Kotlin.

`inputHash` alone decides regeneration: it hashes `PROMPT_VERSION` (`generation/PromptBuilder.kt`,
surfaced as `AiDescriptionGenerator.promptVersion`) plus the normalized, sorted `AppAiContext`.
Bumping `PROMPT_VERSION` when the prompt or output contract changes therefore invalidates every
cached description. Version code is deliberately *not* part of the identity — an app update that
changes nothing the model sees keeps its description; one that adds or removes permissions or
components changes the hash on its own.

This is this repository's first use of Room; `apkanalyzer.room` (`build-logic/convention`) is the
convention plugin other modules should reuse rather than reconfiguring Room per module.

## Privacy

Never log a full prompt or a full generated description — package names are fine (matching
`core:apps` conventions), the content sent to or received from the model is not.
