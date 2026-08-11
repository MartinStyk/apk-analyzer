# core:app-ai-description Module

## Purpose and Boundary

Generates a short, factual, on-device AI summary of one app (installed package or APK file) from
data already produced by `core:apps`, caches it, and hides itself entirely when on-device AI is
unavailable or generation fails. The package is `sk.styk.martin.apkanalyzer.core.appaidescription`.

This module owns no UI. `feature/app-detail/impl` owns the `AiSummaryCard` composable and its
ViewModel; they are the only consumers of `AppAiDescriptionRepository`.

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

## Flow

`getDescription` → `AppDetailMetadataProvider` → normalize → `inputHash` → cache lookup keyed on
`(packageName, versionCode, inputHash)` → on miss, check AI availability → generate → validate →
one stricter-prompt retry on invalid output → cache and return, or return `null`. Concurrent calls
for the same `AppReference` are coalesced (`Mutex` + in-flight `Deferred` map in
`AppAiDescriptionRepositoryImpl`) rather than triggering duplicate generations.

## AI Integration

`generation/MlKitAiDescriptionGenerator` wraps the ML Kit GenAI Prompt API
(`com.google.mlkit:genai-prompt`). It checks `checkStatus()` and returns `null` immediately when
`FeatureStatus.UNAVAILABLE`; `PromptBuilder`/`DescriptionParser`/`DescriptionValidator` build the
structured prompt, parse the (markdown-fence-tolerant) JSON response, and reject output containing
model/implementation leakage. No cloud AI, no API key, ever.

## Cache

Room-backed, single table (`AppAiDescriptionEntity`, PK `packageName`). Cache identity is
`(packageName, versionCode, inputHash)` — any mismatch is a cache miss and triggers regeneration.
This is this repository's first use of Room; `apkanalyzer.room` (`build-logic/convention`) is the
convention plugin other modules should reuse rather than reconfiguring Room per module.

## Privacy

Never log a full prompt or a full generated description — package names are fine (matching
`core:apps` conventions), the content sent to or received from the model is not.
