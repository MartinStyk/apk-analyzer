# feature:app-export Module

## Purpose

A three-step flow that exports a CSV or JSON inventory of installed apps: choose the apps, choose the
details (with presets), then pick the format, check the preview, and save or share. An animated step
indicator shows progress and lets the user jump back. It opens from the Export button beside Sort on the app list.

The API module exposes `InventoryExportNavKey` (all installed apps) and `AppInventoryExportNavKey` (one
app, opened from the app detail summary sheet). Implementation code uses the package
`sk.styk.martin.apkanalyzer.feature.appexport.phone`. This feature has no `tv` module.

## Package Map

* Root - the single destination: `InventoryExportScreen` and its one `InventoryExportViewModel` with
  State/Action/Event. The ViewModel owns step navigation, the intro, the stop dialog, the format, the
  preview, and the export job; whether an export runs is derived from its progress state alone.
* `apps/` and `columns/` - steps 1 and 2. Each has a plain holder (`AppSelection`, `ColumnSelection`)
  that owns the choices and their changes, plus the page. Holders are not ViewModels; the
  ViewModel creates them, forwards the step actions to them, and folds their state into its own.
* `InventoryExportScope` - which apps the destination exports and which steps it shows. `InstalledApps`
  shows all three steps; `SingleApp` pins the selection to one package and drops the apps step, so
  the flow is details then export. The ViewModel takes the scope through its assisted factory, and the
  state carries the step list, so the indicator, Next, Back, and the Export page's apps card follow it.
* `export/` - step 3's page.
* `components/` - the shared step scaffold with its indicator and the labels for `core:app-export` columns, groups,
  presets, and formats.
* `navigation/` - the entry provider.

## State and Navigation Rules

* `ConfigurationKey.InventoryExportEnabled` is observed only at the two phone entry points, not
  inside the export flow. Open or restored flows continue independently of configuration changes.
  APK and icon export are separate capabilities.
* The steps are composables inside one destination, chosen by `InventoryExportState.step`, not
  separate Navigation 3 entries. One ViewModel is scoped to that entry and every page reads its single
  `state` and sends its single `onAction`; step actions are the `AppSelectionAction` and
  `ColumnSelectionAction` branches of `InventoryExportAction`. Nothing is shared through a singleton, so
  nothing needs repair after process death: the destination simply starts again from the list filter
  and the last saved selection.
* The Next button and the ViewModel's step navigation both read `AppSelectionSummary.hasSelection`
  through `InventoryExportState`, so they cannot disagree about whether the selection is valid.
* Back goes to the previous step, leaves from the first, and asks before stopping a running export. Confirming
  cancels and joins the export before navigating back so partial-file cleanup finishes first.
  While an export runs, every action except back, cancel, and the stop dialog is ignored.
  Next is enabled only when the current step has a valid choice. The indicator only jumps to earlier
  steps and is disabled while exporting.
* The app set is "apps matching the export filter, minus the ones the user unticked". The export
  filter is the `AppFilterScope("inventory-export")` repository from `core:apps`, seeded from the list
  filter when the ViewModel is created and edited through `AppFilterNavKey` (the shared filter
  screen). Unticked apps are stored as exclusions, so changing the filter later keeps them. Select
  all and Clear act on the rows currently visible after search.
* Presets (`InventoryPreset`) set the whole column selection; the chip is selected only while the
  selection equals a preset exactly. Columns and format are saved after a successful export and
  restored the next time.
* Columns and format are chosen independently. CSV cannot hold structured columns, so with CSV selected
  the feature leaves them out of the request and the Export step lists which details are left out,
  with a one-tap switch to JSON. The omission is always shown to the user, never silent; an export
  with no writable column left is disabled.
* The preview is computed only while the Export step is showing, so toggling apps or columns on
  earlier steps never loads app data.
* Save carries the format of the document picker that returned it, and the export waits for the
  restored selection and the app list before it starts, so a result delivered after process death
  still writes the file the user created.
* The Export step links to the published file format reference (`INVENTORY_REFERENCE_URL`).
* Export logic, file writing, destination handling, and progress live in `core:app-export`. This
  module maps state to UI and hands the file to the document picker or the share sheet.
* `requiresAppAnalysis` marks every expensive source, including the independent lazy native-library,
  signing-scheme, intent-filter, and manifest queries, not just `AppDetailRepository`.
* First-run guidance: step 1 shows an intro card until the user taps "Got it", which persists through
  `InventoryExportPreferences`. The "About this export" action in the step 1 toolbar brings it back
  without changing the saved flag. While the intro is visible, step 1 hides its one-line guidance,
  because the card covers that job; every other step always shows its guidance line under the step
  bar. A selected preset explains itself with a one-line description on the details step.
* The filter button sits beside the search bar and highlights while a filter is active. Resetting the
  filter happens on the filter screen.
* Each step's content is one scrolling list, with its guidance text and search bar as the first items,
  so nothing above the list pins itself and squeezes it. On short screens (landscape phones, under
  480dp tall) the step indicator also moves into that scrolling list and the bottom bar tightens, so
  only the toolbar and Next stay fixed.
