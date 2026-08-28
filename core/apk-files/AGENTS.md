# core:apk-files Module

## Purpose and Boundary

Owns temporary APK materialization and cleanup for APKs received through Android content URIs. The
package is `sk.styk.martin.apkanalyzer.core.apkfiles`.

This module owns file lifecycle only. APK parsing and analysis belong in `core:apps`; Activities and
ViewModels decide when ownership begins and ends.

## Storage and Lifecycle Semantics

Temporary APKs live under `cacheDir/apk-analysis/task_<taskId>/`, keeping launcher and external
document tasks isolated. Before copying a new APK, cleanup compares those directories with live
`ActivityManager.appTasks` so files orphaned by process death are removed after their task is gone.

Copies stream on the injected IO dispatcher and are capped at 2 GiB. Never trust a provider-reported
size because APK intents can originate from arbitrary content providers. Exceeding the cap throws
`ApkTooLargeException` (declared alongside `TemporaryApkManager`) instead of a plain `IOException`,
so callers can log the rejection without it being mistaken for an unexpected failure.

Call `release(apkFilePath)` when ownership ends. Release must remain idempotent because both the
importing ViewModel and app-detail ViewModel can observe the same Activity teardown.
