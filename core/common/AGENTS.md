# core:common Module

## Purpose and Boundary

Foundation module for infrastructure and models shared across otherwise independent domains. The
package is `sk.styk.martin.apkanalyzer.core.common`.

Keep this module small and dependency-safe. Feature-specific policy, UI components, and app-analysis
logic do not belong here.

## Package Map

* `coroutines/` - injectable dispatchers, flow helpers, and cancellation-safe result capture.
* `logger/` - the repository logging facade.
* `settings/` - generic typed DataStore persistence and preference keys.
* `model/` - genuinely cross-module value types such as app references, source classification, and
  file sizes.
* `resources/`, `clipboard/`, and `digest/` - shared platform adapters and focused utilities.

## Durable Contracts

* Inject `DispatcherProvider`; never hardcode a dispatcher.
* Cancellation-safe result helpers must rethrow coroutine cancellation rather than convert it into
  a failure value.
* Use `Logger`, never Timber or Crashlytics directly outside this module.
* `AppReference` is the shared type-safe distinction between an installed package and an APK file.
* `AppSource.isSideloaded` intentionally groups sideloaded, local-install, and unknown sources while
  excluding recognized stores and system-preinstalled apps.
* Add preference keys to the typed persistence contract rather than creating feature-local
  DataStore instances.
