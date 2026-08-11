# App Loading Observability

**Status:** Proposed
**Scope:** Production logging and performance measurement for navigation, app discovery, app
analysis, and their supporting repository operations

## Requirements

- Log when a loading operation and each meaningful stage starts, succeeds, degrades, or fails.
- Log every screen opening from the visible Navigation 3 destination.
- Use log levels consistently so Crashlytics records unexpected recoverable and terminal failures as
  non-fatals without turning expected states into errors.
- Measure the complete time needed to load an app and attribute that time to non-overlapping stages,
  including manifest parsing, certificate extraction, signing-scheme detection, packaging analysis,
  component mapping, permissions, storage, and usage.
- Keep Firebase Performance behind an app-owned interface. Domain `core` modules and feature modules
  must not import Firebase Performance types.
- Package names may appear in diagnostic logs. APK paths may appear when analyzing an APK file.
  Logging sanitization is outside this rollout.

## Decision

Use two complementary signals:

1. `Logger` and Firebase Crashlytics provide chronological diagnostic breadcrumbs and non-fatal
   reports for unexpected failures.
2. Firebase Performance custom code traces provide aggregate parent durations and per-stage timing
   metrics for successful, degraded, failed, and cancelled loads.

Crashlytics logs are not a general remote log stream. A successful operation log is visible only
when it is attached to a crash or recorded non-fatal event from that app session. Do not create a
non-fatal exception merely to upload successful-operation logs. Firebase Performance is the
production source for successful-load timing and volume trends.

Performance instrumentation belongs at repository and parser/analyzer boundaries, not in
Composables or ViewModels. These boundaries are stable, include cache behavior, and isolate the work
most likely to regress. Screen-opening logs belong in the app navigation host, where the visible
Navigation 3 destination can be observed centrally. The initial metrics measure data availability,
not navigation-to-first-frame latency.

Use one parent trace per public load request and record non-overlapping stage durations as metrics on
that trace. Do not create one nested Firebase trace per stage. Parent traces keep total duration,
attributes, and stage metrics associated with one operation and avoid an unmanageable trace list.

## Logging Payload and Collection

- Logs may contain the package name for an installed app and the APK path for APK-file analysis.
- Do not add a sanitization abstraction or rewrite existing diagnostic context in this rollout.
- Raw throwables may be reported when their failure qualifies as a non-fatal under the logging policy.
- Do not use a user ID.
- Performance trace names, metrics, and attributes remain low-cardinality. Package names, APK paths,
  screen parameters, and other request identities must not become Performance attributes because
  they fragment aggregate distributions.
- Automatic collection is enabled in the current app for debug and release builds. The implementation
  should disable Crashlytics and Performance collection in normal debug builds so local development
  does not pollute production data. Provide an explicit local validation switch or dedicated build
  configuration that enables both SDKs when verifying instrumentation.
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
| `DEBUG` | Operation/stage start, cache hit/miss, expected absence, and successful internal stages | Breadcrumb only |
| `INFO` | Successful completion of a public repository load or a process-level reload | Breadcrumb only |
| `WARN` without `Throwable` | Unusual but expected or fully handled state, such as missing usage permission, an uninstall race, unsupported signing data, or unavailable optional metadata | Breadcrumb only |
| `WARN` with `Throwable` | Unexpected recoverable failure where the operation returns a useful but degraded result | One non-fatal report with diagnostic context |
| `ERROR` with `Throwable` | Unexpected failure that makes the parent operation fail | One non-fatal report with diagnostic context |

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

interface PerformanceTrace {
    fun putMetric(name: String, value: Long)
    fun putAttribute(name: String, value: String)
    fun stop()
}
```

Provide the Firebase implementation and Hilt binding from `app`. Only that adapter imports
`FirebasePerformance` or `Trace`. Domain modules inject `PerformanceTracker` and know only the
contract.

Requirements for the adapter and shared helpers:

- Every `startTrace` call returns an independent handle because repository calls can overlap.
- `stop()` is idempotent and thread safe.
- Fixed trace, metric, and attribute names are declared centrally and satisfy Firebase naming limits.
- Measure stages with a monotonic clock at nanosecond resolution and record whole microseconds in
  metrics suffixed with `_us`. Do not use wall-clock time.
- A shared inline/suspend measurement helper records a stage duration even when the measured block
  throws, then rethrows the original exception or cancellation.
- Parent completion uses `try`/`finally`, records its outcome before stopping, and stops exactly once.
- Instrumentation must not change repository results, exception behavior, cache semantics, dispatcher
  selection, or cancellation propagation.
- Telemetry API failures must be surfaced through the existing safe logging policy but must not turn a
  successful domain operation into a failure.

No Firebase Performance type may appear outside `app`.

## Primary Trace Design

### `installed_apps_load`

Start immediately before querying `PackageManager`. Stop after the basic `InstalledApp` list has
been produced.

| Custom metric | Meaning |
|---|---|
| `package_query_us` | `PackageManager.getInstalledPackages` duration |
| `app_mapping_us` | Mapping all returned `PackageInfo` values to `InstalledApp` |
| `app_count` | Number of successfully mapped apps |

| Attribute | Values |
|---|---|
| `outcome` | `success`, `error`, `cancelled` |
| `trigger` | `initial`, `package_change` |
| `app_count_bucket` | `0_49`, `50_99`, `100_199`, `200_399`, `400_plus` |

### `app_detail_load`

Start at entry to `AppDetailRepository.details`, before either cache lookup. Stop when the repository
returns a complete or degraded `AppDetail`, an error, or cancellation.

| Custom metric | Meaning |
|---|---|
| `cache_lookup_us` | Installed-package or APK-file cache lookup |
| `package_query_us` | Installed-package or APK-archive `PackageManager` lookup |
| `intent_filters_us` | Base and split manifest parsing for component intent filters |
| `storage_stats_us` | Installed-package storage-size lookup |
| `usage_stats_us` | Installed-package last-used lookup |
| `general_info_us` | General metadata and install-source mapping |
| `certificate_us` | Certificate parsing, digests, trust assessment, and self-signature checks |
| `signing_schemes_us` | v1 verification and APK Signing Block scheme detection |
| `launcher_query_us` | Installed-package launcher activity queries |
| `components_us` | Activity, service, provider, and receiver mapping |
| `permissions_us` | Used and defined permission mapping and resolution |
| `features_us` | Required-feature mapping |
| `packaging_us` | APK size, installed splits, and native-library ZIP analysis |

| Attribute | Values |
|---|---|
| `outcome` | `success`, `degraded`, `error`, `cancelled` |
| `analysis_mode` | `installed`, `apk_file` |
| `cache_hit` | `true`, `false` |
| `intent_filters` | `available`, `unavailable` |
| `signing_schemes` | `available`, `unavailable` |

A cache hit records only `cache_lookup_us` and the attributes available on the cached result. Miss-only
metrics are absent rather than zero. Installed-only metrics are absent for APK files.

Refactor `getPackageDetails` into named, sequential, behavior-preserving stage calculations before
constructing `AppDetail`. This is required to prevent overlapping timers and to separate certificate,
signing-scheme, component, and packaging costs precisely.

An optional sub-operation that fails while `AppDetail` remains usable produces `outcome=degraded`.
The trace must retain the stage duration and availability attribute so degraded requests can be
filtered away from complete requests.

### `manifest_load`

This trace measures the complete readable-manifest request from `ManifestParser.manifest`.

| Custom metric | Meaning |
|---|---|
| `resource_lookup_us` | Resolve package/archive information and resources |
| `manifest_parse_us` | Read and parse binary manifest data |
| `xml_render_us` | Render readable namespaced XML |
| `split_count` | Additional installed split count |

| Attribute | Values |
|---|---|
| `outcome` | `success`, `error`, `cancelled` |
| `analysis_mode` | `installed`, `apk_file` |
| `split_count_bucket` | `0`, `1_4`, `5_9`, `10_plus` |

If the current renderer cannot expose parsing separately from rendering without duplicating work,
first refactor it into explicit parse and render stages while preserving output.

## Supporting Trace Design

Add these traces after the three primary traces use the same infrastructure successfully:

| Trace | Metrics | Key attributes |
|---|---|---|
| `app_signing_index_load` | `package_query_us`, `certificate_mapping_us`, `app_count`, `certificate_count` | `outcome`, `trigger`, `app_count_bucket` |
| `storage_stats_load` | `permission_check_us`, `stats_query_us`, `requested_count`, `loaded_count` | `outcome`, `permission`, `trigger` |
| `usage_stats_load` | `permission_check_us`, `usage_query_us`, `usage_mapping_us`, `loaded_count` | `outcome`, `permission`, `trigger` |
| `device_features_load` | `feature_query_us`, `feature_mapping_us`, `feature_count` | `outcome` |

Do not emit one remote trace or non-fatal per installed app from bulk operations. One parent trace
contains aggregate counts and total stage durations. Per-app failures are accumulated into a bounded
failure count; one non-fatal may be recorded for the operation only when the failure is
unexpected and materially degrades the result.

## Reading the Results

Firebase Performance automatically records the parent duration. Custom metrics appear alongside it
as aggregate distributions.

The console can answer:

- What are p50, p90, and p95 for complete and degraded `app_detail_load` requests?
- How much time do manifest intent filters, certificates, and signing-scheme detection contribute?
- How do installed-package and APK-file analysis differ?
- Are cache misses slower in the latest version?
- Does installed-app loading degrade as the app-count bucket increases?
- Are storage or usage enrichments slow only when permission is available?

The Firebase console does not directly provide a nested waterfall, stacked stage chart, or arbitrary
formulas across custom metrics. Stage metrics must be non-overlapping so their sum approximately
explains the parent duration. Small gaps are expected from trace bookkeeping, cache operations,
coroutine dispatch, object construction, and control flow.

Export Performance data to BigQuery only if per-sample percentages, stacked visualizations, or more
advanced correlations become necessary.

Firebase currently permits 32 metrics including the default duration metric and five custom
attributes on one custom trace. Every proposed trace remains below both limits.

## Rollout

### OBS-01: Make logging consistent

- Add the direct human-readable loading message convention.
- Apply the log-level and single-owner non-fatal rules to current load paths.
- Add missing start, success, degraded, and failure logs to the required operation
  coverage.
- Log each `currentKey` directly from both navigation hosts.
- Preserve package names and APK paths where they provide useful diagnostic context.

### OBS-02: Add Performance infrastructure

- Add Firebase-free `PerformanceTracker` and `PerformanceTrace` contracts and monotonic timing helpers
  to `core:common`.
- Add the Firebase-backed adapter and Hilt binding in `app`.
- Add build-aware collection control so regular debug builds do not report production telemetry.
- Confirm independent handles, precise microsecond metrics, idempotent stop, and cancellation safety.
- Verify Crashlytics with one intentional test crash and one intentional non-fatal in the
  explicit monitoring-validation build.
- Verify Performance using Firebase Performance logcat output before instrumenting repositories.

### OBS-03: Instrument installed-app loading

- Add `installed_apps_load`.
- Split package querying from model mapping without changing emitted data.
- Add separate storage and usage enrichment traces.
- Emit one primary trace for initial loading and each package-change reload.

### OBS-04: Instrument app-detail loading

- Add `app_detail_load` around installed-package and APK-file requests.
- Refactor miss processing into the named non-overlapping stages.
- Report cache state, analysis mode, optional-stage availability, and outcome.
- Preserve both cache strategies and cancellation propagation.

### OBS-05: Instrument manifest and supporting loads

- Add `manifest_load` for readable manifests.
- Add the signing-index and device-feature traces.
- Complete the public repository/manager load-entry inventory and document every exclusion.

### OBS-06: Establish baselines and alerts

- Collect at least one representative production release.
- Track p50, p90, and p95 parent and stage metrics.
- Segment app-list results by app-count bucket.
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
- `app_detail_load` separately reports manifest intent filters, certificates, signing schemes,
  components, permissions, features, and packaging.
- Cache hits and misses, installed packages and APK files, and complete and degraded results can be
  compared independently.
- Stage metrics are non-overlapping and approximately explain parent duration.
- No Firebase Performance type leaves `app`.
- Performance attributes contain no package names, APK paths, screen parameters, or other
  high-cardinality request identities.
- Regular debug builds do not pollute production monitoring.

## Deferred

- Compose or ViewModel instrumentation.
- Navigation-to-first-content-frame timing.
- Logging sanitization and diagnostic-data redaction.
- Macrobenchmark and Baseline Profile modules.
- BigQuery dashboards before the Firebase console proves insufficient.
- Additional custom sampling before production volume demonstrates a need.
