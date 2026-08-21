# core:common Module

## Purpose and Boundary

Foundation module for infrastructure and models shared across otherwise independent domains. The
package is `sk.styk.martin.apkanalyzer.core.common`.

Keep this module small and dependency-safe. Feature-specific policy, UI components, and app-analysis
logic do not belong here.

## Package Map

* `applanguage/` - reads the per-app language setting (API 33+ `LocaleManager`) behind
  `AppLanguageRepository`.
* `coroutines/` - injectable dispatchers, flow helpers, and cancellation-safe result capture.
* `logger/` - the repository logging facade.
* `performance/` - performance instrumentation contracts and the internal Firebase adapter.
* `review/` - in-app review eligibility tracking and the internal Play Core adapter.
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
* Crashlytics only receives INFO and above (`Logger.d`/`Logger.v` never reach it). Its `.log()` buffer
  is a limited-size breadcrumb trail sent only alongside a crash or non-fatal, not a live stream —
  DEBUG-level per-stage detail belongs in local logcat only, or it crowds out the breadcrumbs that
  matter by the time a crash actually happens.
* Firebase Performance imports stay inside the internal adapter in this infrastructure module.
  Domain core modules and feature modules depend only on `PerformanceTracker` and
  `PerformanceTrace`. This module already owns the Firebase-backed logging facade, while the app
  convention plugin remains responsible for packaging and instrumenting the SDK.
* Time an expensive stage with `PerformanceTrace.measuredSection` (records a metric, no logging) or
  `PerformanceTrace.timedSection` (same, plus a paired DEBUG started/finished log) instead of calling
  `measureTimedValue` and setting the trace metric by hand at each call site.
* `AppReference` is the shared type-safe distinction between an installed package and an APK file.
* `AppSource.isSideloaded` intentionally groups sideloaded, local-install, and unknown sources while
  excluding recognized stores and system-preinstalled apps.
* Add preference keys to the typed persistence contract rather than creating feature-local
  DataStore instances.
* Play Core review APIs stay inside `review/`'s internal adapter, same rule as Firebase Performance.
  `ReviewEligibilityTracker.reviewPromptRequests` is a one-shot event stream (`Flow<Unit>`), not
  observable state — showing the review dialog is a side effect, and this codebase's rule that
  one-shot signals go through an event channel rather than state applies here too, even though this
  tracker sits below the ViewModel layer. It deliberately has no cooldown or lifetime cap beyond
  "ask once" — Play's own quota governs repeat frequency, and reimplementing that policy locally is
  unnecessary.
