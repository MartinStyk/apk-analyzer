# core:apk-files Module

## Purpose
Owns temporary APK materialization and cleanup for APKs supplied through Android content URIs.

## Package: `sk.styk.martin.apkanalyzer.core.apkfiles`

## Structure

```
TemporaryApkManager.kt       - Public task-scoped copy and release contract
TemporaryApkManagerImpl.kt   - Copies URI streams on IO and removes cache directories for discarded tasks
di/
  TemporaryApkModule.kt      - Singleton Hilt binding
```

## Storage Model

Temporary APKs live under `cacheDir/apk-analysis/task_<taskId>/`. Before each copy, the manager
queries `ActivityManager.appTasks` and removes directories whose task no longer exists. Live main
and external document tasks therefore keep independent files, while files orphaned by process
death are removed by the next APK analysis after their task is discarded.

Call `release(apkFilePath)` when ownership ends. Release is idempotent because both the importing
ViewModel and app-detail ViewModel may observe the same Activity teardown.

## Dependencies

- `core:common` for `DispatcherProvider` and `Logger`
- Hilt
