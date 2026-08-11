# core:common Module

## Purpose and Boundary

Foundation module for infrastructure and models shared across otherwise independent domains. The
package is `sk.styk.martin.apkanalyzer.core.common`.

Keep this module small and dependency-safe. Feature-specific policy, UI components, and app-analysis
logic do not belong here.

## Package Map

* `coroutines/` - injectable dispatchers, flow helpers, and cancellation-safe result capture.
* `logger/` - the repository logging facade.
* `performance/` - Firebase-free performance instrumentation contracts, stage-timing helpers, and
  centralized telemetry names.
* `settings/` - generic typed DataStore persistence and preference keys.
* `model/` - genuinely cross-module value types such as app references, source classification, and
  file sizes.
* `resources/`, `clipboard/`, and `digest/` - shared platform adapters and focused utilities.

## Durable Contracts

* Inject `DispatcherProvider`; never hardcode a dispatcher.
* Cancellation-safe result helpers must rethrow coroutine cancellation rather than convert it into
  a failure value.
* Use `Logger`, never Timber or Crashlytics directly outside this module.
* Use severity-specific `Logger` methods with plain, readable messages. Do not add typed logging
  events, formatter helpers, or logging-only wrapper functions.
* Throwable-bearing WARN and ERROR logs record Crashlytics non-fatals. Attach the throwable once at
  the boundary that owns the degraded result or terminal failure.
* `PerformanceTracker.startTrace(name) { trace -> ... }` runs the block against a `PerformanceTrace`
  and stops it automatically, including on exceptions or cancellation. Callers only call
  `putMetric`/`putAttribute` inside the block; they never manage `stop()` themselves.
* `measureStage(...)` / `measureSuspendStage(...)` record a whole-microsecond stage metric on a
  `PerformanceTrace` using a monotonic nanosecond clock, regardless of how the timed block completes.
* `PerformanceTraceName` / `PerformanceMetricName` / `PerformanceAttributeName` are the only source of
  telemetry name strings; do not inline new trace, metric, or attribute names elsewhere.
* No Firebase Performance type may appear outside `app`.
* `AppReference` is the shared type-safe distinction between an installed package and an APK file.
* `AppSource.isSideloaded` intentionally groups sideloaded, local-install, and unknown sources while
  excluding recognized stores and system-preinstalled apps.
* Add preference keys to the typed persistence contract rather than creating feature-local
  DataStore instances.
