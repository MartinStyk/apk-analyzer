# core:common Module

## Purpose and Boundary

Foundation module for infrastructure and models shared across otherwise independent domains. The
package is `sk.styk.martin.apkanalyzer.core.common`.

Keep this module small and dependency-safe. Feature-specific policy, UI components, and app-analysis
logic do not belong here.

## Package Map

* `coroutines/` - injectable dispatchers, flow helpers, and cancellation-safe result capture.
* `logger/` - the repository logging facade.
* `performance/` - performance instrumentation contracts, the internal Firebase adapter,
  stage-timing helpers, and centralized telemetry names.
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
* `PerformanceTracker.startTrace(name)` returns an `AutoCloseable` `PerformanceTrace`. Always scope
  it with `use` so it closes on success, failure, and cancellation.
* `measureStage(...)` / `measureSuspendStage(...)` record a whole-microsecond stage metric on a
  `PerformanceTrace` using a monotonic nanosecond clock, regardless of how the timed block completes.
* `PerformanceTraceName` / `PerformanceMetricName` / `PerformanceAttributeName` are the only source of
  telemetry name strings; do not inline new trace, metric, or attribute names elsewhere.
* Firebase Performance imports stay inside the internal adapter in this infrastructure module.
  Domain core modules and feature modules depend only on `PerformanceTracker` and
  `PerformanceTrace`. This module already owns the Firebase-backed logging facade, while the app
  convention plugin remains responsible for packaging and instrumenting the SDK.
* `AppReference` is the shared type-safe distinction between an installed package and an APK file.
* `AppSource.isSideloaded` intentionally groups sideloaded, local-install, and unknown sources while
  excluding recognized stores and system-preinstalled apps.
* Add preference keys to the typed persistence contract rather than creating feature-local
  DataStore instances.
