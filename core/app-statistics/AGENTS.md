# core:app-statistics Module

## Purpose
Computes aggregate device-wide statistics across all installed apps — counts/percentages by system-app status, install location, target/min SDK, install source, signing algorithm, plus mean/median/min/max/variance/std-deviation for APK size and component counts. A one-shot, progress-reporting batch computation (not a live-updating repository) powering a device statistics dashboard.

## Package: `sk.styk.martin.apkanalyzer.core.appstatistics`

## Structure

```
LocalApplicationStatisticManager.kt  - @WorkerThread class; iterates all installed apps, builds StatisticsData, emits progress via Flow
model/
  StatisticsData.kt                  - @Parcelize data class: full aggregated statistics result (counts + MathStatistics per metric)
  MathStatistics.kt                  - @Parcelize data class (mean/median/max/min/variance/deviation) + internal FloatArray.toMathStats() extension
  PercentagePair.kt                  - @Parcelize data class (count, percentage) with PercentagePair.from(count, total) factory
util/
  BigDecimalFormatter.kt             - object; lazily-cached DecimalFormat singleton (2-2 fraction digits)
```
No `di/` folder — `LocalApplicationStatisticManager` is a public class with an `internal @Inject` constructor, injected directly without an interface/binding.

## Key Classes

```kotlin
@WorkerThread
class LocalApplicationStatisticManager @Inject internal constructor(
    private val packageManager: PackageManager,
    private val installedAppsRepository: InstalledAppsRepository,
    private val certificateExtractor: CertificateExtractor,
    private val installSourceResolver: InstallSourceResolver,
) {
    sealed class StatisticsLoadingStatus {
        data class Loading(val currentProgress: Int, val totalProgress: Int) : StatisticsLoadingStatus()
        data class Data(val data: StatisticsData) : StatisticsLoadingStatus()
    }

    fun loadStatisticsData(): Flow<StatisticsLoadingStatus>
}
```

## Known Bugs Worth Knowing Before Touching This Module

- **`PercentagePair.from(count, total)`** computes `percentage = BigDecimal(count + 100 / total)` — integer division (`100 / total` truncates) added to `count`. This is not the percentage formula (`count / total * 100`) and looks like a genuine bug, not a deliberate design choice. Don't trust displayed percentages without verifying this first if a statistics bug is reported.
- **`StatisticsDataBuilder`** (private) pre-sizes its `FloatArray`s to `datasetSize + 1` and uses a running success counter as the write index — index `0` of every array is always left as an unused default `0f`. Since `toMathStats()` sorts and includes that leading zero, `min`/mean/median are all skewed by one phantom zero entry. Looks like an off-by-one in the sizing, not intentional padding.
- **`BigDecimalFormatter`** caches a single shared mutable `DecimalFormat` at object level. `DecimalFormat` is documented as not thread-safe in the JDK — concurrent formatting from multiple threads could corrupt output. Not currently an issue since this manager runs single-threaded per invocation, but would break if statistics formatting were ever parallelized.
- **`MathStatistics.toMathStats()`** sorts the array in place, throws on empty input, and computes variance with Bessel's correction (`/ (size - 1)`) — a size-1 dataset divides by zero.

## Other Notable Implementation Details

- `loadStatisticsData()` re-runs the *entire* per-app analysis from scratch every time `installedAppsRepository.apps()` emits (via `flatMapLatest`) — any install/uninstall on the device restarts the whole computation. No caching between calls, unlike `core:app-permissions`.
- Per-app analysis calls `packageManager.getPackageInfo(packageName, ANALYSIS_FLAGS)` with a wide flag set (`GET_SIGNING_CERTIFICATES or GET_ACTIVITIES or GET_SERVICES or GET_PROVIDERS or GET_RECEIVERS or GET_PERMISSIONS`) — comparatively expensive per app. Failures are caught, logged, and counted in `analyzeFailed` rather than thrown.
- `@WorkerThread`-annotated — this module does **not** switch dispatchers internally (unlike `core:app-permissions`'s `flowOn`); callers must ensure it isn't invoked on the main thread.
- Reaches into `core:apps` internals beyond its public repository surface: `CertificateExtractor`, `InstallSourceResolver`, and analysis helpers from `sk.styk.martin.apkanalyzer.core.apps.analysis`.

## Dependencies
- `apkanalyzer.library` + `apkanalyzer.hilt` + `parcelize` plugins
- `implementation(projects.core.apps)`
- `implementation(projects.core.common)`
