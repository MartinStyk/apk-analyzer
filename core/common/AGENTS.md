# core:common Module

## Purpose
Foundation module with shared utilities depended on by nearly all other modules. Provides coroutine infrastructure, logging, persistence, resources, and shared models.

## Package: `sk.styk.martin.apkanalyzer.core.common`

## Structure

```
coroutines/
  DispatcherProvider.kt   - Injectable dispatcher provider (main, default, io, unconfined)
  Flows.kt                - Flow utility extensions
  RunCatching.kt          - Result capture for suspend code that rethrows cancellation
logger/
  Logger.kt               - Timber + Firebase Crashlytics logging wrapper (object)
  OperationLog.kt         - `nextOperationRequest()` process-local request-id counter and
                            `operationLogMessage(...)` shared `operation=<op> request=<n> [stage=<s>]
                            event=<e> [context]` message-shape formatter for load-operation logging
performance/
  PerformanceTracker.kt   - Firebase-free parent-trace and trace-handle contracts
  PerformanceTiming.kt    - Elapsed-realtime nanosecond clock and sync/suspend stage timing helpers
  PerformanceNames.kt     - Central low-cardinality trace, metric, and attribute names
resources/
  ResourcesManager.kt     - Android resources access (strings, colors, dimensions)
settings/
  PersistenceRepository.kt        - Interface for DataStore preferences
  DataStorePersistenceRepository.kt - Implementation
  PersistenceModule.kt             - Hilt module
  Key.kt                           - Preference key definitions (includes ColorAppScheme)
model/
  AppReference.kt         - Installed-package or APK-file reference shared across analysis and UI
  AppSource.kt            - Enum: GooglePlay, SamsungGalaxyStore, AmazonAppstore, HuaweiAppGallery,
                            XiaomiGetApps, FDroid, AuroraStore, Sideloaded, LocalInstall,
                            SystemPreinstalled, Unknown. `isSideloaded` groups Sideloaded/LocalInstall/
                            Unknown — the "not from a store, not system" cluster the Filter and
                            App detail screens key their "Sideloaded" quick filter/badge off of
  AppSize.kt              - Value class for file sizes with formatting
clipboard/                - Clipboard access utilities
digest/                   - Hash/digest utilities
util/                     - General Android and formatting utilities
```

## Key Exports

- `DispatcherProvider` - Inject in ViewModels/Repositories for coroutine dispatching
- `Logger` - Static logging: `Logger.d("Tag", "msg")`, `Logger.e("Tag", throwable, "msg")`
- `nextOperationRequest()` / `operationLogMessage(...)` - Consistent operation/stage/event logging
  convention for public load operations across repositories
- `PerformanceTracker` / `PerformanceTrace` - Firebase-free performance instrumentation contracts
- `measureStage(...)` / `measureSuspendStage(...)` - Exception- and cancellation-safe whole-microsecond
  stage timing with a monotonic nanosecond clock
- `PerformanceTraceName` / `PerformanceMetricName` / `PerformanceAttributeName` - Fixed production
  telemetry names
- `ResourcesManager` - Injectable Android resources access
- `PersistenceRepository` - DataStore preferences abstraction
- `AppSource` - App install source classification
- `AppReference` - Type-safe reference to an installed package or an APK file
- `AppSize` - File size value with display formatting
- `ColorAppScheme` - Day/Night/FollowSystem enum

## Dependencies
- Firebase Crashlytics (for Logger)
- Timber
- DataStore Preferences
- Hilt
