# core:app-export Module

## Purpose and Boundary

Custom installed-app inventory export: the user picks apps and columns, and this module writes them as
CSV or JSON to a user-chosen document or to a share file. The package is
`sk.styk.martin.apkanalyzer.core.appexport`.

It is a consumer of public `core:apps` repositories (`InstalledAppsRepository`, `AppDetailRepository`,
`AppSigningRepository`) and owns no `PackageManager` access. Single-app APK and icon export stays in
`core:apps` `export/`. The filter that picks the exported apps is the `@InventoryExportFilter`
`AppFilterRepository` in `core:apps` `filtering/`, because the shared filter screen edits it.

## Attributes

* `InventoryAttribute` is the stable catalog: enum order is the column order in every format, and
  `key` is the CSV header and JSON field name. Renaming a `key` breaks users' spreadsheets and
  scripts, so add a new entry instead.
* Each attribute declares its `InventoryDataSource`. Only `AppAnalysis` attributes trigger a
  per-app `AppDetailRepository.details()` call; keep cheap columns on `InstalledApp` or the signing
  index so an export without them stays fast.
* `requiresUsageAccess` attributes resolve against `awaitFullyEnrichedApps()`; without usage access
  they stay empty instead of failing the export.
* `InventoryValue` keeps values typed until a format writes them: CSV joins lists with `; `, JSON
  keeps arrays, numbers, and nulls.
* CSV cells that start with a spreadsheet formula character are prefixed with `'`, because app
  names and labels are untrusted input that a spreadsheet would otherwise execute.

## Export Semantics

* `export()` emits `Running(0, total)` first, then one `Running` per app, then exactly one
  `Completed` or `Failed`. An app whose analysis fails is still written with empty analysis columns
  and counted in `incompleteAppCount`; it never fails the whole export.
* `ExportDestinationWriter` truncates the destination and deletes it when writing fails or is
  cancelled, so a cancelled export never leaves a partial file behind.
* `createShareDestination()` recreates `cache/inventory/` for every share so at most one shared
  inventory stays on disk. The URI uses the app's `FileProvider` (authority = application ID), whose
  `cache-path` must keep covering that directory.
* `preview()` renders the first few apps through the same writer as the real export, so the preview
  can never disagree with the file.
