# App Loading Observability

**Status:** OBS-01 through OBS-03 implemented; later rollout stages proposed
**Scope:** Production logging and performance measurement for navigation, app discovery, app
analysis, and their supporting repository operations

## Requirements

- Log when a loading operation and each meaningful stage starts, succeeds, degrades, or fails.
- Log every screen opening from the visible Navigation 3 destination.
- Use log levels consistently so Crashlytics records unexpected recoverable and terminal failures as
  non-fatals without turning expected states into errors.
- Measure the complete time needed for each top-level load operation.
- Keep Firebase Performance behind a shared infrastructure interface. Domain `core` modules and
  feature modules must not import Firebase Performance types.
- Package names may appear in diagnostic logs. APK paths may appear when analyzing an APK file.
  Logging sanitization is outside this rollout.

## Decision

Use two complementary signals:

1. `Logger` and Firebase Crashlytics provide chronological diagnostic breadcrumbs and non-fatal
   reports for unexpected failures.
2. Firebase Performance custom code traces provide aggregate duration distributions for successful,
   degraded, failed, and cancelled loads.

Crashlytics logs are not a general remote log stream. A successful operation log is visible only
when it is attached to a crash or recorded non-fatal event from that app session. Do not create a
non-fatal exception merely to upload successful-operation logs. Firebase Performance is the
production source for successful-load timing and volume trends.

Performance instrumentation belongs at repository and parser/analyzer boundaries, not in
Composables or ViewModels. These boundaries are stable, include cache behavior, and isolate the work
most likely to regress. Screen-opening logs belong in the app navigation host, where the visible
Navigation 3 destination can be observed centrally. The initial metrics measure data availability,
not navigation-to-first-frame latency.

Use one trace per public load request. Firebase records its total duration automatically. Add only
bounded attributes that materially segment the result; do not time internal calls or stages by
default. This keeps instrumentation readable and avoids an unmanageable metric list.

## Logging Payload and Collection

- Logs may contain the package name for an installed app and the APK path for APK-file analysis.
- Do not add a sanitization abstraction or rewrite existing diagnostic context in this rollout.
- Raw throwables may be reported when their failure qualifies as a non-fatal under the logging policy.
- Do not use a user ID.
- Performance trace names, metrics, and attributes remain low-cardinality. Package names, APK paths,
  screen parameters, and other request identities must not become Performance attributes because
  they fragment aggregate distributions.
- Crashlytics and Performance collection are enabled in debug and release builds.
- Keep Firebase's normal SDK sampling behavior. Do not build a second custom sampling layer until
  production volume demonstrates a need.

## Logging Design

### Message shape

Use short human-readable messages directly with the severity-specific `Logger` method:

```text
<Operation or stage> loading <started|finished|degraded|failed>: <result or context>
```

Context may include analysis mode, cache state, bounded counts, package name, or APK path.

Every parent operation logs one start and exactly one terminal event. A stage logs a start and one
terminal event when it performs meaningful I/O, parsing, cryptography, or bulk mapping. Do not log
inside per-item loops; report a bounded count when the stage finishes.

### Log levels

| Level | Use | Crashlytics behavior |
|---|---|---|
| `DEBUG` | Operation/stage start, cache hit/miss, expected absence, and successful internal stages | Local logcat only, not forwarded |
| `INFO` | Successful completion of a public repository load or a process-level reload | Breadcrumb only |
| `WARN` without `Throwable` | Unusual but expected or fully handled state, such as missing usage permission, an uninstall race, unsupported signing data, or unavailable optional metadata | Breadcrumb only |
| `WARN` with `Throwable` | Unexpected recoverable failure where the operation returns a useful but degraded result | One non-fatal report with diagnostic context |
| `ERROR` with `Throwable` | Unexpected failure that makes the parent operation fail | One non-fatal report with diagnostic context |

`FirebaseTree` (`core:common`) drops anything below `INFO` before it reaches
`FirebaseCrashlytics.log(...)`. Crashlytics's log buffer is small and only sent alongside a crash or
non-fatal, so per-stage DEBUG detail — now emitted routinely by `timedSection` across every timed
repository — would otherwise crowd out the breadcrumbs that matter by the time a crash happens.

Do not use `ERROR` without a `Throwable` for a failed operation because it cannot produce the required
non-fatal report. Do not report coroutine cancellation as a warning, error, or non-fatal.

### Non-fatal ownership

Record one non-fatal for one failure. The layer that consumes an error owns the report:

- If a parser or analyzer catches an exception and returns a degraded value, that component logs one
  `WARN` with the throwable. The parent logs `degraded` without the throwable.
- If an exception propagates and fails the public repository request, lower layers do not report it.
  The repository boundary logs one `ERROR` with the throwable.
- Expected platform outcomes represented by the domain API, including missing permission, missing
  optional metadata, unsupported signing data, and package removal during a query, do not become
  non-fatals.

This prevents the same stack trace from appearing as several Crashlytics issues.

### Required operation coverage

The first implementation covers these operation families:

| Operation | Meaningful stages |
|---|---|
| Installed apps | Package query, app mapping, storage enrichment, usage enrichment |
| App detail | Cache lookup, package/archive query, component intent filters, storage, usage, general information, certificates, signing schemes, launcher lookup, component mapping, permissions, features, packaging |
| Readable manifest | Package/resources lookup, binary manifest parsing, XML rendering |
| Component intent filters | Package/resources lookup, base and split manifest parsing, resource resolution, grouping |
| Device-wide signing | Package query, certificate extraction and assessment |
| Storage stats | Permission check, package-stat queries, result mapping |
| Usage stats | Permission check, usage query, result mapping |
| Device features | Platform query and domain mapping |

The rollout must also inventory every public repository or manager load entry point in `core/*`.
Each entry point must be explicitly classified as instrumented, covered by a parent operation, or too
small/deterministic to justify a remote Performance trace. Logging still applies when a separate
Performance trace is not justified.

### Screen opening

Observe the visible destination once in each app navigation host:

```kotlin
LaunchedEffect(navigationState.currentKey) {
    Logger.i("Navigation", "Screen opened: ${navigationState.currentKey}")
}
```

The effect emits the initially visible destination and restarts for each actual `currentKey` change.
Log the `NavKey` directly; every key is a data object or data class so its representation identifies
the destination and includes its diagnostic parameters without a separate resolver.

Emit one `INFO` breadcrumb in this shape:

```text
Screen opened: <NavKey>
```

Cover both the main `ApkAnalyzerApp` host and the external-APK navigation host. This phase adds
screen-opening logs only; navigation-to-first-content timing remains separate.

## Current Loading Pipelines

### Installed app list

```text
InstalledAppsRepositoryImpl
  PackageManager.getInstalledPackages(GET_PERMISSIONS)
  map PackageInfo to InstalledApp
    resolve install source and category
    read basic APK size and package metadata
  emit basic list
  request storage-size enrichment
  combine storage and usage data into later emissions
```

`InstalledAppsRepositoryImpl` is eagerly shared for the lifetime of the process. Its primary trace
measures process-level basic-list availability, not the time from entering `AppsScreen` until drawing
the first content frame.

Storage and usage enrichment arrive independently and must not extend `installed_apps_load`.
They receive their own traces because permissions, platform services, and the number of installed
apps give them different performance characteristics.

### App detail

```text
AppDetailRepositoryImpl.details(reference)
  cache lookup for installed packages and APK files
  PackageManager package/archive query
  parse base and split manifests for component intent filters
  optional storage-size query
  optional usage query
  general information mapping
  certificate extraction and assessment
  APK signing-scheme detection
    verify v1 JAR signature
    parse v2/v3/v3.1 APK Signing Block IDs
  launcher activity query for installed packages
  activity, service, provider, and receiver mapping
  permission mapping and resolution
  feature mapping
  APK size and installed-split analysis
  native-library ZIP analysis
  cache result
```

Installed-package details are cached by package name until a package-change event clears the cache.
APK-file details are cached by absolute path and last-modified time.

Component intent-filter parsing is part of app-detail loading and must be included in
`app_detail_load`. Rendering the complete readable manifest belongs to the separate Manifest screen
and uses `manifest_load`.

## Performance Integration

Declare the Firebase-free contract in `core:common`:

```kotlin
interface PerformanceTracker {
    fun startTrace(name: String): PerformanceTrace
}

interface PerformanceTrace : AutoCloseable {
    operator fun set(name: String, value: Long)
    operator fun set(name: String, value: String)
}

inline fun <T> PerformanceTrace.measuredSection(metric: String, block: () -> T): T

var PerformanceTrace.outcome: TraceOutcome
    get() = throw UnsupportedOperationException(...)
    set(value) { this["outcome"] = ... }

var PerformanceTrace.permission: TracePermission
    get() = throw UnsupportedOperationException(...)
    set(value) { this["permission"] = ... }

enum class TraceOutcome {
    Success,
    Degraded,
    Error,
    Cancelled,
}

enum class TracePermission {
    Granted,
    Denied,
}
```

Provide the internal Firebase implementation and Hilt binding from `core:common`. This module is the
shared infrastructure boundary and already owns the Firebase-backed logging facade; its
`firebase-performance` dependency is not exposed to consumers. The application convention plugin
continues to package the SDK and apply Firebase Performance instrumentation. Only the internal
adapter imports `FirebasePerformance` or `Trace`; domain modules and features inject
`PerformanceTracker` and know only the contract.

Requirements for the adapter and contract:

- `startCancellableTrace` starts and scopes an independent trace handle. It records the standard
  `outcome=cancelled` attribute and closes the handle when the block returns, throws, or is cancelled.
- `trace[name] = longValue` records a metric, while `trace[name] = stringValue` records an attribute.
- Shared values use typed setters such as `trace.outcome = TraceOutcome.Success` and
  `trace.permission = TracePermission.Granted`; operation-specific attributes stay local. Both are
  write-only extension properties; reading either throws.
- Wrap a timed step with `trace.measuredSection(metric) { ... }` instead of `measureTimedValue`; it
  records the duration as a metric and returns the block's value.
- Each emitting repository owns private operation-specific names that satisfy Firebase naming limits.
- Callers record the parent outcome attribute inside the trace block before it returns or throws.
- Instrumentation must not change repository results, cache semantics, dispatcher selection, or
  cancellation propagation.
- The adapter calls Firebase directly. It does not catch SDK `RuntimeException`s or add logging-only
  wrappers; failures propagate to the operation boundary that owns failure logging.

No Firebase Performance type may appear outside the internal `core:common` adapter.

## Primary Trace Design

### `installed_apps_load`

Start immediately before querying `PackageManager`. Stop after the basic `InstalledApp` list has
been produced. The automatic trace duration includes package querying and model mapping.

| Custom metric | Meaning |
|---|---|
| `package_query_ms` | `PackageManager.getInstalledPackages` duration in whole milliseconds |
| `app_count` | Number of successfully mapped installed apps |

| Attribute | Values |
|---|---|
| `outcome` | `success`, `error`, `cancelled` |

### `app_detail_load`

Start at entry to `AppDetailRepository.details`, before either cache lookup. Stop when the repository
returns a complete or degraded `AppDetail`, an error, or cancellation.

| Attribute | Values |
|---|---|
| `outcome` | `success`, `degraded`, `error`, `cancelled` |
| `analysis_mode` | `installed`, `apk_file` |
| `cache_hit` | `true`, `false` |
| `intent_filters` | `available`, `unavailable` |
| `signing_schemes` | `available`, `unavailable` |

An optional sub-operation that fails while `AppDetail` remains usable produces `outcome=degraded`.
Availability attributes allow degraded requests to be filtered away from complete requests.
The trace intentionally has no custom stage metrics: Firebase already measures the total request, and
the existing chronological logs identify internal stages without adding permanent remote telemetry.

OBS-04 uses the two availability facts persisted in `AppDetail` to classify complete versus degraded
results: component intent filters and signing schemes. Storage size and last-used time remain nullable
domain values that currently combine permission denial, query failure or race, and legitimate absence,
so they cannot safely affect the parent outcome or be reconstructed on a cache hit. Likewise,
`NativeLibraries.Empty` combines an APK with no native libraries and a handled ZIP-read failure, while
certificate extraction exposes an empty result for both legitimate absence and handled failure.
`ApkSigningBlockAnalyzer` returns `null` for both no detectable supported scheme and analyzer failure;
therefore `signing_schemes=unavailable` is an availability statement, not a failure diagnosis. OBS-04
does not speculate beyond these APIs or change their established result behavior.

### `manifest_load`

This trace measures the complete readable-manifest request from `ManifestParser.manifest`.
`componentIntentFilters` (the other public method on `ManifestParser`) has exactly one caller —
`AppDetailRepositoryImpl` — and is already fully covered by that caller's `app_detail_load` stage
timing, so it does not own a second trace or duplicate logging of its own.

| Attribute | Values |
|---|---|
| `outcome` | `success`, `error`, `cancelled` |
| `analysis_mode` | `installed`, `apk_file` |

`split_count_bucket` was planned here but dropped: bucketing split count only adds segmentation value
once a production question needs it, and nothing does yet.

## Supporting Trace Design

Add these traces after the three primary traces use the same infrastructure successfully:

| Trace | Key attributes | Custom metrics |
|---|---|---|
| `app_signing_index_load` | `outcome` | `package_query_ms`, `signing_extraction_ms` |
| `storage_stats_load` | `outcome`, `permission`, `trigger` | `storage_stats_query_ms` |
| `usage_stats_load` | `outcome`, `permission` | `usage_stats_query_ms` |
| `app_index_build` | `outcome` | `app_count`, `index_build_ms` |
| `device_features_load` | `outcome` | — (not yet instrumented; single memoized `PackageManager` call, low value) |
| `ai_summary_load` | `outcome`, `analysis_mode`, `cache_hit`, `availability` | — |

`app_signing_index_load` has one reload path (package-change driven), so a `trigger` attribute would
never vary and was dropped — it wouldn't segment anything. The current enrichment entry points do not
carry the installed-list trigger through their public repository APIs. Storage reports
`trigger=installed_apps` for list-driven requests and `trigger=lifecycle_start` for foreground
refreshes. Usage has only lifecycle-driven bulk refreshes, so it does not record a constant trigger
attribute; its single-package query remains part of app-detail work. Both enrichment traces report
`permission=granted|denied` and use `outcome=degraded` when permission is missing or a storage query
yields partial data.

`app_index_build` (`core:app-index`) times the CPU-bound attribute-bucketing pass over the combined
installed-app and signing-index flows — not IO-bound, but expensive enough across the full app list
plus per-app certificates to be worth one aggregate duration metric, matching this doc's "only time
genuinely expensive stages" rule.

`ai_summary_load` (`core:ai-insights`) wraps one coalesced generation per `AppReference`, not every
`getDescription` call — concurrent callers for the same reference share one trace. `analysis_mode`
is `installed` or `apk_file`; only `installed` requests are cache-eligible, so `cache_hit` is only
meaningful there. `availability` mirrors `AiAvailability` (`available`, `downloadable`,
`downloading`, `unavailable`) at the moment generation was attempted. `outcome=degraded` covers every
expected reason `getDescription` returns `null` — AI not available, metadata lookup failure, or no
valid model output after the retry — since none of those are unexpected errors; `outcome=error` is
reserved for a genuine unhandled exception (for example, a cache I/O failure).

Do not emit one remote trace or non-fatal per installed app from bulk operations. Per-app failures
are accumulated for logging; one non-fatal may be recorded for the operation only when the failure
is unexpected and materially degrades the result.

## Reading the Results

Firebase Performance automatically records each trace duration. Bounded attributes segment those
duration distributions.

The console can answer:

- What are p50, p90, and p95 for complete and degraded `app_detail_load` requests?
- How do installed-package and APK-file analysis differ?
- Are cache misses slower in the latest version?
- How much of installed-app loading is spent in the package-manager query?
- How many installed apps are successfully mapped?
- Are storage or usage enrichments slow only when permission is available?

If a parent trace is slow, use the chronological stage logs and local profiling to identify the
cause. Add a remote custom metric only after a concrete production question justifies its code cost.

## Rollout

OBS-01 through OBS-04 are implemented. OBS-05 is partially implemented — see below. OBS-06 remains
deferred.

### OBS-01: Make logging consistent

- Add the direct human-readable loading message convention.
- Apply the log-level and single-owner non-fatal rules to current load paths.
- Add missing start, success, degraded, and failure logs to the required operation
  coverage.
- Log each `currentKey` directly from both navigation hosts.
- Preserve package names and APK paths where they provide useful diagnostic context.

### OBS-02: Add Performance infrastructure

- Add Firebase-free `PerformanceTracker` and `PerformanceTrace` contracts to `core:common`.
- Add the internal Firebase-backed adapter and Hilt binding in `core:common`.
- Enable collection in debug and release.
- Confirm independent handles, `use`-scoped closure, and cancellation safety.

### OBS-03: Instrument installed-app loading

- Add `installed_apps_load`.
- Split package querying from model mapping without changing emitted data.
- Add separate storage and usage enrichment traces.
- Emit one primary trace for initial loading and each package-change reload.

### OBS-04: Instrument app-detail loading

- Add `app_detail_load` around installed-package and APK-file requests.
- Report cache state, analysis mode, optional-stage availability, and outcome.
- Preserve both cache strategies and cancellation propagation.

### OBS-05: Instrument manifest and supporting loads

- Add `manifest_load` for readable manifests. Done.
- Add the signing-index trace. Done — `app_signing_index_load` also fixed a latent bug where
  `AppSigningRepositoryImpl` rethrew from a fire-and-forget `shareIn` collector with no
  `CoroutineExceptionHandler`; it now logs and falls back to an empty index instead.
- Add `app_index_build` (`core:app-index`) for the device-wide attribute index. Done — not in the
  original plan, added because the grouping pass is CPU-bound over the full app list and per-app
  certificates.
- Add the device-feature trace. Deferred — `DeviceFeaturesRepositoryImpl` is one memoized
  `PackageManager` call, not a loop; low value relative to the others.
- Complete the public repository/manager load-entry inventory and document every exclusion. Deferred.

### OBS-06: Establish baselines and alerts

- Collect at least one representative production release.
- Track p50, p90, and p95 trace durations.
- Segment app-detail results by analysis mode, cache hit, completeness, and signing availability.
- Review Crashlytics non-fatals for duplicate ownership and noisy expected states.
- Set regression thresholds and alerts only after observing real distributions.

### OBS-07: Reassess perceived loading

Add navigation-to-first-content measurement only if repository duration does not explain user reports
or frame-level regressions. Keep that trace separate from repository traces.

## Exit Criteria

- Every in-scope load operation has start and terminal logs with stable operation and stage names.
- Every distinct visible destination change emits one screen-opening log in the applicable navigation
  host.
- Expected states and cancellation do not create Crashlytics non-fatals.
- Every unexpected failure produces at most one non-fatal.
- Firebase reports duration distributions for all primary and supporting traces.
- Cache hits and misses, installed packages and APK files, and complete and degraded results can be
  compared independently.
- No Firebase Performance type leaves the internal `core:common` adapter.
- Performance attributes contain no package names, APK paths, screen parameters, or other
  high-cardinality request identities.
- Debug and release builds both collect monitoring data; no validation entry points ship in the app.

## Deferred

- Compose or ViewModel instrumentation.
- Navigation-to-first-content-frame timing.
- Logging sanitization and diagnostic-data redaction.
- Macrobenchmark and Baseline Profile modules.
- BigQuery dashboards before the Firebase console proves insufficient.
- Additional custom sampling before production volume demonstrates a need.
