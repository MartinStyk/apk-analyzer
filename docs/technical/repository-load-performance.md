# Repository Load Performance

**Status:** Proposed
**Scope:** Production measurement of installed-app list and app-detail repository load times

## Decision

Use Firebase Performance custom code traces inside the repositories. Do not add performance
tracking to Composables or ViewModels in the first iteration.

The initial metrics intentionally measure data-loading time, not perceived screen-loading time.
Repository instrumentation is simpler, has stable boundaries, avoids UI lifecycle coupling, and
isolates the work most likely to regress. UI-to-first-frame measurement can be added later if
repository improvements stop correlating with user experience.

Use one parent trace per repository request and record non-overlapping stage durations as custom
metrics on that trace. This keeps the total duration and its breakdown associated with the same
operation.

## Current Loading Pipelines

### Installed app list

```text
InstalledAppsRepositoryImpl
  PackageManager.getInstalledPackages(GET_PERMISSIONS)
  map PackageInfo to InstalledApp
  emit basic list
  request storage-size enrichment
  combine storage and usage data into later emissions
```

`InstalledAppsRepositoryImpl` is eagerly shared for the lifetime of the process. Its trace therefore
measures process-level data availability, not the time from entering `AppsScreen` until drawing its
first content frame.

The first iteration should measure production of the basic app list. Storage and usage enrichment
arrive independently and should not extend the primary trace. Otherwise device permissions and the
number of installed apps would make "list loaded" mean different things for different users.

### App detail

```text
AppDetailRepositoryImpl.details(reference)
  cache lookup for installed packages
  PackageManager package/archive query
  optional storage-size query
  optional usage query
  general information mapping
  certificate extraction
  launcher activity query
  component mapping
  permission mapping and resolution
  feature mapping
  cache result for installed packages
```

Installed-package details are cached by package name until a package-change event clears the cache.
APK-file details are not cached.

The main app-detail load does not explicitly parse `AndroidManifest.xml`. Readable manifest parsing
belongs to the separate Manifest screen and must not be reported as part of app-detail load time.
`PackageManager` internally reads package metadata, but that work belongs to the package-query stage.

## Firebase Trace Design

### `installed_apps_load`

Start immediately before querying `PackageManager`. Stop after the basic `InstalledApp` list has
been produced.

| Custom metric | Meaning |
|---|---|
| `package_query_ms` | `PackageManager.getInstalledPackages` duration |
| `app_mapping_ms` | Mapping all returned `PackageInfo` values to `InstalledApp` |
| `app_count` | Number of successfully mapped apps |

Attributes:

| Attribute | Values |
|---|---|
| `outcome` | `success`, `error`, `cancelled` |
| `app_count_bucket` | `0_49`, `50_99`, `100_199`, `200_399`, `400_plus` |

`app_count` is useful as a numeric metric for correlation. The bucket is useful for filtering in
the Firebase console without creating one attribute value per exact count.

### `app_detail_load`

Start at entry to `AppDetailRepository.details`, before the installed-package cache lookup. Stop
when the repository has produced either a complete `AppDetail` or an error.

| Custom metric | Meaning |
|---|---|
| `package_query_ms` | Installed-package or APK-archive `PackageManager` lookup |
| `detail_mapping_ms` | General information, components, permissions, and features |
| `certificate_ms` | Signing certificate extraction and assessment |

Attributes:

| Attribute | Values |
|---|---|
| `outcome` | `success`, `error`, `cancelled` |
| `analysis_mode` | `installed`, `apk_file` |
| `cache_hit` | `true`, `false`, `not_applicable` |

The first iteration deliberately keeps the breakdown broad. A cache hit should have a very short
parent duration and no miss-only stage metrics. Storage and usage lookups remain part of total
duration without their own metrics initially. After collecting a production baseline, split
`detail_mapping_ms` or add storage and usage metrics only when the data shows that more detail would
help diagnose a regression.

Do not attach package names, app names, APK paths, permission names, certificate data, or other
high-cardinality or user-identifying values.

## Reading the Results

Firebase Performance automatically records the parent trace duration. Custom metrics appear
alongside that duration and can be inspected as aggregate distributions.

The Firebase console does not provide an automatic nested waterfall or stacked breakdown. It can
answer questions such as:

- What are p50, p90, and p95 for `app_detail_load`?
- How does `package_query_ms` differ between installed-package and APK-file analysis?
- Are cache misses slower in the latest app version?
- Does installed-app load degrade as the app-count bucket increases?

It does not directly calculate:

- what percentage of each request was spent in every stage;
- whether stage durations add exactly to the parent duration;
- a stacked chart of the stage breakdown;
- arbitrary formulas across custom metrics.

Stage metrics should be non-overlapping so their sum approximately explains the parent duration.
Small unmeasured gaps are expected from trace bookkeeping, cache operations, coroutine dispatch,
and control flow.

Export Firebase Performance data to BigQuery if per-sample percentages, stacked visualizations, or
more advanced correlations become necessary. Separate nested custom traces are not required for the
first iteration and would make it harder to analyze one operation as a single record.

Firebase currently permits up to 32 metrics, including the default duration metric, and up to five
custom attributes on one custom trace. The proposed traces remain below both limits.

## Integration Design

Feature and core modules must not depend directly on Firebase APIs. Add a small tracing abstraction
to `core:common` and provide its Firebase implementation from `app`.

Conceptual API:

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

The production implementation wraps Firebase Performance `Trace`. Repository code measures stages
with a monotonic clock and records whole milliseconds. Trace completion must be protected with
`try`/`finally` so success, failure, and coroutine cancellation all stop the trace exactly once.

Do not store active traces globally by name. Repository calls can overlap, so every
`startTrace` invocation must return an independent handle.

## Rollout

### PT-01: Add tracing infrastructure

- Add `PerformanceTracker` and `PerformanceTrace` to `core:common`.
- Add the Firebase-backed implementation and Hilt binding in `app`.
- Ensure trace handles are independent and stop safely.
- Confirm collection in a Firebase-enabled debug build before instrumenting repositories.

### PT-02: Instrument installed-app loading

- Split package querying from model mapping without changing behavior.
- Add the `installed_apps_load` trace and metrics.
- Keep storage and usage enrichment outside this trace.
- Confirm one trace is emitted for initial process loading and each package-change reload.

### PT-03: Instrument app-detail loading

- Add `app_detail_load` around repository requests.
- Report cache state, analysis mode, and outcome.
- Measure package querying, detail mapping, and certificate extraction.
- Preserve cache behavior and cancellation propagation.

### PT-04: Establish baselines

- Collect at least one representative release of production data.
- Track p50, p90, and p95 parent duration and stage metrics.
- Segment app-list results by app-count bucket.
- Segment app-detail results by analysis mode and cache hit.
- Set regression thresholds only after observing real distributions; do not invent absolute service
  levels before a baseline exists.

### PT-05: Reassess UI measurement

Add screen-level first-content traces only if repository duration does not adequately explain user
reports or frame-level regressions. That later work should measure navigation-to-first-content frame
and remain separate from repository traces.

## Exit Criteria

- Firebase reports `installed_apps_load` and `app_detail_load` duration distributions.
- Every trace records an outcome and the applicable low-cardinality attributes.
- Stage metrics provide an approximate, non-overlapping explanation of parent duration.
- Cache hits and misses can be compared independently.
- No Firebase type crosses the `app` module boundary.
- No package, app, or file identity is included in performance telemetry.

## Deferred

- Compose or ViewModel instrumentation.
- Navigation-to-first-frame timing.
- Storage and usage enrichment traces.
- Fine-grained app-detail mapping stages.
- Manifest-screen parsing traces.
- Macrobenchmark and Baseline Profile modules.
- BigQuery dashboards before the Firebase console proves insufficient.
