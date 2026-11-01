# feature:inventory-export Module

## Purpose

A three-step wizard that exports a CSV or JSON inventory of installed apps: pick the apps, pick the
columns, then save or share the file. It opens from the Export button beside Sort on the app list.

The API module exposes only `InventoryExportNavKey`, which opens the first step. Implementation code
uses the package `sk.styk.martin.apkanalyzer.feature.inventoryexport.phone`. This feature has no
`tv` module.

## Package Map

* `draft/` - `InventoryExportDraft`, the `@Singleton` holding the wizard's choices (filter, columns,
  format) shared by all three steps.
* `apps/`, `columns/`, `export/` - one Navigation 3 destination per step, each with its own
  State/Action/Event/ViewModel set.
* `components/` - the shared scaffold with the step indicator, the step enum, and the labels for
  `core:app-export` attributes and formats.
* `navigation/` - the entry provider and the internal keys for the columns and export steps.

## State and Navigation Rules

* Each step is a separate destination with its own `HiltViewModel`, so the steps share state only
  through `InventoryExportDraft`. Never pass selections through nav keys or between ViewModels.
* The apps step seeds the `@InventoryExportFilter` repository from the list filter when its
  ViewModel is created, i.e. once per wizard opening. Columns and format stay in the draft for the
  process lifetime so a repeated export starts from the last choice.
* Editing the selection reuses the apps feature's filter screen through
  `AppFilterNavKey(AppFilterScope.InventoryExport)`; this feature never edits the list filter.
* Tapping an earlier step in the indicator pops back to it — the steps are always stacked in order,
  so the number of pops is the difference of the step ordinals.
* Back during a running export cancels it instead of leaving; the step indicator and format choice
  are disabled while the file is written.
* The draft does not survive process death. When Navigation restores the columns or export step
  without a started draft, that step returns to the apps step so the selection is made again.
* Export logic, file writing, and progress live in `core:app-export`. This module only maps state to
  UI and hands the file to the document picker or the share sheet.
