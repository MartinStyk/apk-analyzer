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

Expensive device-wide signing work is shared lazily. Single-app signing-scheme inspection stays in
the app-detail path so merely collecting the device-wide signer index does not read every APK signing
block.

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
including cache hits. Every internal stage (package query, certificates, signing schemes,
permissions, packaging, ...) records its own `<stage>_ms` metric via `timedStage`, alongside the
bounded analysis mode, cache result, availability, and terminal outcome attributes; package names and
APK paths remain local log context, not trace attributes. Component intent-filter parsing is not one
of these stages: it is fetched lazily and separately by `IntentFiltersRepository`, which owns its own
`intent_filters_load` trace, because only the Components and Intent Filters screens ever need that
data — the hub only needs component counts. The `packaging` stage itself only runs a cheap
existence check (`hasNativeLibraries`); the full per-library breakdown is fetched lazily and
separately by `NativeLibrariesRepository`, which owns its own `native_libraries_load` trace, because
only the general-info and native-libraries screens need per-ABI/per-file detail — the hub only needs
to know whether any native code ships at all.

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

Native-library data follows a lighter version of the same boundary: the cached app model records only
whether the APK ships native code at all (`AppDetail.hasNativeLibraries`). The full shipped ABI/library
set comes from `NativeLibrariesRepository` instead, fetched lazily by the screens that need it; compare
it with `Build.SUPPORTED_ABIS` at that consuming layer, not by promoting the full set into `AppDetail`.

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
cannot diverge.

Read native libraries from base and split ZIP entries. Keep one file record per library name and ABI;
derive distinct ABI and library-name summaries rather than storing competing copies.

`AppDetail.hasNativeLibraries` is a cheap, short-circuiting existence check computed eagerly on every
app-detail load. The full per-file breakdown (`NativeLibraries`, with size and containing-APK per
library) is expensive to build and only two screens need it, so it is not part of `AppDetail` at all —
fetch it lazily and separately through `NativeLibrariesRepository`.

Install-source models preserve the installing, initiating, and originating chain. Source
classification is a pure function over that chain and app flags, not another platform resolver call.
