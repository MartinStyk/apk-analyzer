# feature:what-changed Module

## Purpose

Top-level "What changed" destination: a device-wide view of app history recorded by
[`core:app-history`](../../core/app-history/AGENTS.md) (roadmap `HI-14`, with the `HI-07` full change
log). Phone only and debug only: the tab is shown when `ConfigurationKey.AppHistoryDisplayEnabled` is
on. There is no Pro gate, lock UI, first-run screen, or badge yet.

The API module exposes `WhatChangedNavKey` and the tab label. The phone implementation uses the
package `sk.styk.martin.apkanalyzer.feature.whatchanged.phone`. There is no TV module.

## Package Map

* The root package owns the tab: State/Action/Event/ViewModel/Screen, the selectable
  `WhatChangedRange`, and `WhatChangedVisitTracker`.
* `log/` owns the full change log ("Full history"), opened from the tab's top bar.
* `components/` owns `AppChangeItem` (one change or first-recorded entry for one app) with its
  injected mapper, the row composables both screens share, and the loading/error/empty content.
* `navigation/` owns the entry provider and the internal log key. Rows open the per-app timeline,
  change detail, and historical app detail through the keys in `feature:app-detail:api`.

## Rules

* **All history rules live in `core:app-history`** (`AppHistoryDeviceSummary.kt`): counts, net change,
  needs-attention, revert dates. The ViewModels only choose the range and map to UI models; never
  recompute "needs attention" or a count here, or the tab and the per-app timeline will disagree.
* **The range filters the summary, Needs attention and Changed apps.** No longer installed and the
  full log always cover all recorded history. Latest change appears only when the range has no
  changed apps: otherwise it would repeat the first Changed apps or Needs attention row.
* **"Since last visit" means the previous app run that opened the tab.** `WhatChangedVisitTracker`
  reads and replaces the stored visit once per process, so reopening the tab in the same run never
  moves the start. Never visited means all recorded history. There is no 7-day floor, so "No changes
  since …" is a valid summary.
* **Changed apps show the net change over the range** (state before the range vs now), so a
  permission added and removed again reads "No net change". Needs attention stays per event and marks
  an undone event with its revert date.
* Section rows can repeat an app across sections, so LazyColumn keys carry the section prefix.
