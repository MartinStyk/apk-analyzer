# core:apps Module

## Purpose and Boundary

Core domain module for installed-app and APK analysis. It owns platform extraction, normalization,
caching, and the domain models consumed by features. The package is
`sk.styk.martin.apkanalyzer.core.apps`.

Expose repositories and typed models, not Android framework structures. Features may interpret the
domain data for presentation but must not repeat extraction or security policy.

## Domain Package Map

The module uses package-by-domain organization. Each domain package keeps its repository or resolver,
models, implementation, and private analysis helpers together.

* `model/` - the small set of app-level models composed across domains.
* `signing/` - certificates, signer history, trust, algorithms, and signing-scheme inspection.
* `permissions/` - one app's declared and used permissions and typed protection metadata.
* `components/` - activities, services, receivers, providers, intent filters, and path permissions.
* `manifest/` - binary manifest parsing and readable XML rendering.
* `intentfilters/` - cached, on-demand lookup of parsed component intent filters, built on
  `manifest/`'s `ManifestParser`.
* `installsource/` - raw installer chain and source classification.
* `devicefeatures/` - device capabilities and app/device requirement comparison.
* `packaging/` - installed splits, native-library existence and breakdown, and APK size.
* `usagestats/` and `storagestats/` - permission-gated device statistics.
* `export/` - APK and icon export.
* `sdkversion/` - Android version labels.

Keep the Hilt bindings grouped by these domains. A new independent repository family earns its own
domain package; do not return to flat `analysis/`, `model/`, or `util/` grab-bags.

## Repository and Cache Boundaries

`InstalledAppsRepository` is the live source for installed apps. `AppDetailRepository` orchestrates
the domain analyzers for one installed package or APK file.

App-detail cache entries describe the app only. Do not fold device-dependent availability into a
cached `AppDetail`; consumers combine app facts with device repositories separately.

`PackageChangesObserver` invalidates installed-package data. APK-file inputs and device state have
different lifecycles and must not be smuggled into that cache key.

`PackageChangesObserverImpl.observe()` is one `BroadcastReceiver` shared across every caller
(`shareIn(appScope, SharingStarted.WhileSubscribed(), replay = 0)`), not a fresh registration per
subscriber — independent consumers each registering their own receiver for the same broadcasts was
the previous, wasteful shape. `InstalledAppsRepositoryImpl`, `AppSigningRepositoryImpl`, and
`core:app-history`'s fast-path capture are genuine reactive consumers of the event stream and still
subscribe via `observe()`; each subscribes permanently at construction (`shareIn(appScope,
Eagerly/Lazily, ...)` or `.launchIn(appScope + ...)`), so in practice the shared receiver stays
registered for the app's lifetime; a future consumer that subscribes and later fully unsubscribes
would see the receiver tear down and re-register around that gap, which `replay = 0` makes safe to
reason about (a late subscriber only ever sees events after it attaches).

`AppDetailRepositoryImpl`, `IntentFiltersRepositoryImpl`, `NativeLibrariesRepositoryImpl`, and
`SigningSchemeRepositoryImpl` don't need the event stream at all — they only ever clear their cache
on any change — so they call `PackageChangesObserver.runBeforeNotifying { cache.clear() }` instead of
subscribing to `observe()`. This isn't just a simpler call site: it fixes a real race. Their old
`.onEach { cache.clear() }.launchIn(...)` collectors ran as independent coroutines with no ordering
guarantee relative to any other `observe()` collector reacting to the same broadcast — including
`core:app-history`'s fast-path capture, which could read a stale cached `AppDetail` before the
invalidating collector got scheduled, and permanently persist it under the new install timestamp.
`runBeforeNotifying`'s registered actions run synchronously inside `onReceive`, before `trySend`, so
every cache is guaranteed clear before the event is observable by anyone — a structural
happens-before, not a probabilistic one from coroutine scheduling.

Expensive device-wide signing work is shared lazily. Single-app signing-scheme inspection is itself
expensive (it reads the APK's signing block directly) and only the Certificates screen shows it, so
it is fetched lazily and separately by `SigningSchemeRepository` rather than being collected as part
of the device-wide signer index or the app-detail path.

## Loading Observability

Use direct, readable messages in the form `<operation> loading <started|finished|degraded|failed>`
with useful result counts or context. INFO marks successful public-load completion, WARN marks
degraded results, and ERROR marks terminal failures.

DEBUG marks the start and finish of an internal stage only when that stage is itself expensive — a
platform/IPC query, a file or manifest read, a loop that calls into the platform once per item. A
pure in-memory mapping over data already fetched does not earn a DEBUG pair. Use
`PerformanceTrace.timedSection` (`core:common`) for every such stage instead of logging and setting a
trace metric by hand: it logs the started/finished pair and records the duration as a `<metric>`
trace attribute in one call. `AppDetailRepositoryImpl.timedStage` is a thin wrapper around it for that
file's `"App detail <stage> loading"` wording; repositories with only one or two timed stages
(`InstalledAppsRepositoryImpl`, `StorageStatsRepositoryImpl`, `UsageStatsRepositoryImpl`) call
`timedSection` directly. If a stage can also degrade to a fallback, add a WARN for that outcome on
top of the timing — don't stack a WARN onto every stage, only the ones that actually can degrade.

Attach a throwable only for unexpected recoverable degradation or terminal failure, and only at the
layer that owns that outcome. Let coroutine cancellation propagate without logging it. Package names
and APK file paths are valid diagnostic context.

`AppDetailRepositoryImpl` owns one `app_detail_load` trace around the complete public request,
including cache hits. Every internal stage (package query, certificates, permissions, packaging,
...) records its own `<stage>_ms` metric via `timedStage`, alongside the bounded analysis mode,
cache result, and terminal outcome attributes; package names and APK paths remain local log context,
not trace attributes. Component intent-filter parsing is not one of these stages: it is fetched
lazily and separately by `IntentFiltersRepository`, which owns its own `intent_filters_load` trace,
because only the Components and Intent Filters screens ever need that data — the hub only needs
component counts. Native library detail and signing-scheme detection follow the same pattern: they
are fetched lazily and separately by `NativeLibrariesRepository` (`native_libraries_load` trace) and
`SigningSchemeRepository` (`signing_scheme_load` trace) respectively, because only the
general-info/native-libraries screens and the Certificates screen need that detail — the hub does
not need any of it.

Use `startCancellableTrace` for trace lifetime and cancellation outcome. Classify cached and uncached
results from facts persisted in `AppDetail`; nullable storage, usage, certificate, and packaging data
must not be promoted into guessed telemetry outcomes.

## Signing Semantics

* `apkContentsSigners` is the current signer set. Multiple current signers form one package identity
  and cannot use signing-key rotation.
* For one current signer, `signingCertificateHistory` is Android's verified rotation lineage in
  oldest-to-current order. Keep current and historical certificates as separate lists.
* Historical certificates are previously trusted keys. Do not claim they can sign normal updates;
  capabilities depend on lineage metadata Android does not expose here.
* Preserve X.509 validity as `Instant`. Convert only for display and compare exact instants.
* Equal issuer and subject means self-issued, not self-signed. Call a certificate self-signed only
  after verifying its signature with its own public key.
* Signature-algorithm assessment describes the digest only. Unsupported names stay unknown, and
  overall security also depends on key type and size.
* `SigningInfo` does not expose APK signing schemes. Scheme detection reads the APK directly,
  verifies v1 through the JAR verifier, and parses recognized v2/v3/v3.1 signing-block IDs.
* Structural ambiguity or verification failure yields unknown scheme data, never a guessed list.

## Device Requirement Semantics

`AppDetail` describes what an app requires; device features describe what this device provides.
Consumers combine the two while mapping UI state.

Device feature discovery is memoized because the platform feature set cannot change without reboot.
Read the complete set once rather than calling `hasSystemFeature` repeatedly.

Hardware feature names and OpenGL ES versions are different domain variants. Device support can be
available, unavailable, or unknown; a failed device query must not become "missing."

Native-library data follows a stricter version of the same boundary: it is not part of `AppDetail` at
all. The full shipped ABI/library set comes from `NativeLibrariesRepository` instead, fetched lazily
by the screens that need it; compare it with `Build.SUPPORTED_ABIS` at that consuming layer.

## Manifest and Component Semantics

`PackageInfo` does not enumerate declared intent filters. Query APIs only return filters matching an
intent the caller already knows, so manifest parsing is required.

Parse the base manifest and every installed split manifest. Normalize relative component class names
before keying filters by `ComponentIntentFilterKey`, and include component kind in the key because
different kinds may share a class name. `IntentFiltersRepository` returns this keyed map as-is;
joining it to a specific component list is a feature-layer concern, since only the Components and
Intent Filters screens need it.

Preserve actions, categories, data rules, priority, order, and link-verification requests. Multiple
`<data>` elements accumulate within one filter. Host and port are one authority rule; flattening them
independently creates invalid cross-products. Ignore a port without a host, matching Android.

Split parsing is all-or-nothing. If any required split manifest fails, return failure rather than a
partial result that callers could misread as "no filters."

## External Reachability Semantics

These are technical facts for component filtering, not a general security verdict.

* Exported alone is not a finding.
* Launcher activities are expected to be exported and are excluded from the installed-app
  "unprotected" classification.
* An installed activity is unguarded only when launcher status is known to be false and no permission
  is required.
* Services and receivers are unguarded when exported without a required permission.
* A provider is unguarded when top-level read or write access is open or any path permission opens a
  path.
* APK-file activities have unknown launcher status. Their technical filter may include the launcher;
  features must not promote that uncertainty into a finding.

`ProviderInfo.pathPermissions` is already available when providers are queried. Model each path,
match type, and optional read/write permission. A path entry with neither permission opens that path.

Path permissions can add exposure but cannot prove an otherwise open provider is guarded: uncovered
paths remain subject to the top-level permission.

## Packaging Semantics

Compute installed APK size from the same split list exposed to consumers so totals and breakdowns
cannot diverge. Split-APK size and classification live as private helpers in
`AppDetailRepositoryImpl`, their only consumer.

Read native libraries from base and split ZIP entries. Keep one file record per library name and ABI;
derive distinct ABI and library-name summaries rather than storing competing copies. This logic lives
as private helpers in `NativeLibrariesRepositoryImpl`, their only consumer — none of it is part of
`AppDetail`. The per-file breakdown (`NativeLibraries`, with size and containing-APK per library) is
expensive to build and only two screens need it, so it is fetched lazily and separately through
`NativeLibrariesRepository` rather than computed eagerly on every app-detail load.

Install-source models preserve the installing, initiating, and originating chain. Source
classification is a pure function over that chain and app flags, not another platform resolver call.
