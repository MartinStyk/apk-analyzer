# core:app-export Module

## Purpose and Boundary

Custom installed-app inventory export: the caller picks apps and columns, and this module writes
them as CSV or JSON to a document the user chose or to a share file it creates. The package is
`sk.styk.martin.apkanalyzer.core.appexport`.

It is a consumer of public `core:apps` repositories (`InstalledAppsRepository`, `AppDetailRepository`,
`AppSigningRepository`) and owns no `PackageManager` access. Single-app APK and icon export stays in
`core:apps` `export/`. Which apps are exported is the caller's decision; the feature filters them
through a scoped `AppFilterRepositories` repository from `core:apps`.

## Package Map

* Root: the public API (`InventoryExportManager`, request, destination, preview, progress) and the
  manager implementation, which only orchestrates the packages below.
* `column/`: the column catalog (`InventoryColumn`, group, shape), `InventoryPreset`, `InventoryValue`,
  and the mappers that turn loaded app data into a column's value. A column's definition and its
  value mapping live together.
* `source/`: `InventoryDataSource` and `InventoryAppDataLoader`, which gather each app's data from
  the `core:apps` repositories for the requested sources only. It knows nothing about columns.
* `format/`: `InventoryFormat` and one `InventoryWriter` per format.
* `preferences/`: the last column and format choice, persisted through `core:common` settings.

Dependencies run one way: `source` has none, `column` uses `source`, `format` uses `column`, and the
manager uses all three.

Document I/O uses `core:common`'s `DocumentFileManager`. A caller passes an `InventoryDestination`:
`Document` for a user-chosen URI, or `Share`, for which the manager creates the file through
`core:common`'s `ShareFileManager`. Callers never create files themselves.

## Columns

* `InventoryColumn` is the stable catalog: enum order is the column order in every format, and
  `key` is the CSV header and JSON field name. Renaming a `key` breaks users' spreadsheets and
  scripts, so add a new entry instead. Entries stay grouped by `InventoryColumnGroup` in group order.
* Exported values are as stable as keys. Enum-backed values go through the explicit `token`
  mappings in `InventoryTokens.kt`, never an enum's `name`, so renaming a Kotlin enum never changes
  users' data. Fingerprints use the user's selected format in every column and record; source values
  remain raw hex so either format can be reconstructed without duplicate storage.
* Each column declares its `InventoryDataSource`, and the manager hands the loader only the sources the
  selected columns declare. Native libraries, signing schemes, intent filters, and readable
  manifests each use their existing lazy repository independently of app-detail analysis.
  Every source result is a `SourceResult`: `NotRequested`, `Failed`, or `Loaded`. An app is complete
  unless some source `Failed`, and that one predicate feeds `incompleteAppCount`.
  `AppAnalysis` columns trigger a per-app `AppDetailRepository.details()` call; keep cheap columns on
  `InstalledApp` or the signing index so an export without them stays fast.
* `UsageStats` columns resolve against `awaitFullyEnrichedApps()`; without usage access they stay
  empty instead of failing the export.
* `InventoryValue` keeps values typed until a format writes them: CSV joins simple text lists with
  `; ` and accepts only scalar values and text lists. JSON keeps objects, arrays, numbers,
  booleans, and nulls. Missing data is distinct from an empty collection.
* `InventoryColumnShape` says whether a column is `Scalar` or `Structured` (nested records).
  `InventoryFormat.supports()` is the one compatibility predicate for UI, preview, export, and the CSV
  writer: CSV accepts only scalar columns. Incompatible requests fail before loading app data or
  opening the destination. The module never filters columns itself: a caller that drops structured
  columns for CSV must do it explicitly and tell the user. Never serialize nested records into CSV
  cells.
* `InventoryPreset` names the common column sets. Presets are data, not UI; the feature labels them.
* Counts and full component records are separate columns. Full records preserve permissions,
  flags, provider path rules, and component-keyed intent filters. Requirements preserve both
  hardware and OpenGL ES variants, versions, and required/optional status.
* Certificate records include every certificate/public-key fingerprint, principals, validity dates,
  serial number, algorithm, trust level, and self-signed status. Current and historical certificates
  stay separate. Individual fingerprint columns always describe current signers.
* CSV cells that start with a spreadsheet formula character are prefixed with `'`, because app
  names and labels are untrusted input that a spreadsheet would otherwise execute.

## Export Semantics

* `InventoryWriter` is `AutoCloseable` and owns its supplied `Writer`. Use it in `use`; `close()`
  finalizes the document and releases the writer, including when finalization fails. Write failures
  and cancellation still propagate so the document helper deletes partial output.
* `export()` emits `Running(0, total)` first, then one `Running` per app, then exactly one
  `Completed` (with the final URI and display name) or `Failed`. An app whose analysis fails is still
  written with empty analysis columns and counted in `incompleteAppCount`; it never fails the whole
  export. Failures in selected lazy sources and unavailable signing schemes are also counted as
  incomplete.
* `DocumentFileManager.writeOrDeleteOnFailure()` truncates the destination and deletes it when writing fails or is
  cancelled, so a cancelled export never leaves a partial file behind.
* `preview()` renders the first few apps through the same writer as the real export, so the preview
  can never disagree with the file. Only the displayed preview is bounded by line and character
  limits; exports retain the complete data, including large manifests and record lists.
* JSON is one object: `$schema` (the published schema URL), `schema_version`
  (`INVENTORY_SCHEMA_VERSION`), `generated_at`, and the `apps` array. CSV has no envelope.
* `InventoryExportPreferences` stores the last columns and format by column `key` and format name, and
  whether the feature's intro was dismissed.
  Unknown keys are dropped on read, and an empty or unreadable selection reads as "nothing saved".

## Published Reference

The user-facing contract lives in [`site/inventory-export/`](../../site/inventory-export/index.md):
a readable reference and versioned JSON Schemas (JSON Schema 2020-12). The `Pages` workflow publishes
`site/` to GitHub Pages from `develop`, at the address in `INVENTORY_REFERENCE_URL`; the feature
links to it from the Export step.

* Any change to a column, key, token, record field, or nullability updates the schema and the
  reference in the same change. `PublishedSchemaTest` validates exported values against the current
  schema version, and checks column order, enum tokens, and the schema version and address. Nothing
  checks the reference page, so keep it in step by hand.
* Additive changes (a new column, record field, or token) stay in the current version. A change
  that breaks existing scripts bumps `INVENTORY_SCHEMA_VERSION` and adds a new `vN/` schema;
  published versions are never changed incompatibly. Version 1 supports uppercase continuous and
  colon-separated fingerprints selected in Settings.

## Testing

`site/inventory-export/v1` is a test resource folder, so Gradle re-runs `PublishedSchemaTest` when only
the schema changes. A new `vN/` schema moves that folder and the test to the new version.

`JsonInventoryWriter` uses `android.util.JsonWriter`, so `JsonInventoryWriterTest` and
`PublishedSchemaTest` run with Robolectric. `PublishedSchemaTest` validates the real writer's output.
When a domain enum gains a value, its fixture picks it up through `entries`.
