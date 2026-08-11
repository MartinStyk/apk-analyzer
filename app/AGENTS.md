# app Module

## Purpose and Boundary

The top-level Android application module wires `core/*` and `feature/*/impl` modules together. It
owns application and Activity setup, app-scoped bindings, top-level navigation hosts, and manifest
entry points. Put no feature or analysis logic here.

This module uses the root package `sk.styk.martin.apkanalyzer`; unlike every other module, it has no
module suffix.

## Wiring Rules

`ui/ApkAnalyzerApp.kt` is the assembly point for the main Navigation 3 host. Every reachable feature
must register its `*Entries()` function in that file. `AppsNavKey` is the start destination, and the
top-level keys define the independent bottom-navigation stacks.

The external-APK Activity has its own Navigation 3 host and document task. Keep its temporary APK
ownership and back-stack lifecycle independent from the launcher Activity.

Both navigation hosts log `navigationState.currentKey` directly at INFO whenever the visible
destination changes. Keep navigation keys readable as data classes or data objects; do not add a
screen-name mapping layer.

Both activities use the shared theme host so the persisted color scheme is applied before feature
content renders. Material3 is allowed directly here only for app-shell plumbing such as `Scaffold`
and theme hosting.

Firebase collection policy belongs in the application manifest, not a library manifest. Crashlytics
and Firebase Performance collection are enabled in debug and release. Validate real traces and
non-fatals in Firebase after their repository instrumentation lands; do not keep intentional crash
entry points in the app.

## Manifest Contracts

* The launcher Activity is the exported `MAIN`/`LAUNCHER` entry point.
* The external-APK Activity is an exported document entry point for APK content and runs in an
  isolated document task.
* The file provider is not exported and grants URI permissions through the application ID-based
  authority.
* Package visibility and usage-access permissions are intentional product requirements; preserve
  their lint suppressions when editing the manifest.

Version name and code have local Gradle-property defaults. CI and release workflows override them;
do not introduce a second version source.
