# feature:what-changed Module

## Purpose

Top-level "What changed" destination: a device-wide view of app history recorded by
[`core:app-history`](../../core/app-history/AGENTS.md) (roadmap `HI-14`, with the `HI-07` full change
log). Phone only and debug only: the tab is shown when `FeatureFlag.WhatChangedTab` is
on. It stays debug only until the release that swaps the `noop` entitlements binding for real
billing. Without Pro, most rows are locked behind the paywall in
[`feature:pro`](../pro/AGENTS.md); see [Pro gate](#pro-gate).

The API module exposes `WhatChangedNavKey` and the tab label. The phone implementation uses the
package `sk.styk.martin.apkanalyzer.feature.whatchanged.phone`. There is no TV module.

## Package Map

* The root package owns the tab: State/Action/Event/ViewModel/Screen, the selectable
  `WhatChangedRange`, and `WhatChangedVisitTracker`.
* `log/` owns the full change log ("Full history"), opened from the tab's top bar.
* `access/` owns the Pro gate: `WhatChangedAccessPolicy`, the `WhatChangedAccess` it produces, and
  `LockableRow`, the readable-or-locked wrapper every row in both screens' state goes through.
* `components/` owns `AppChangeItem` (one change or first-recorded entry for one app) with its
  injected mapper, the row composables both screens share (including `LockedRow`), and the
  loading/error/empty content.
* `navigation/` owns the entry provider and the internal log key. Rows open the per-app timeline,
  change detail, and historical app detail through the keys in `feature:app-detail:api`; locked rows
  open `ProNavKey` from `feature:pro:api`.

## Rules

* **All history rules live in `core:app-history`** (`AppHistoryDeviceSummary.kt`): counts, net change,
  needs-attention, revert dates. The ViewModels only choose the range and map to UI models; never
  recompute "needs attention" or a count here, or the tab and the per-app timeline will disagree.
* **The range filters the summary, Needs attention and Changed apps.** No longer installed and the
  full log always cover all recorded history. There is no "Latest change" section: it only repeated
  rows already listed. An empty range names the date of the last change instead, so the user knows
  how far to widen it.
* **The opening range depends on Pro.** Without Pro the tab always opens on All time, so the preview
  shows the most rows; a picked range lasts for that screen only and is never stored. With Pro it
  opens on the stored range, or Since last visit, and a picked range is stored. The opening range is
  chosen once per screen, so buying Pro on the tab doesn't narrow it. `WhatChangedRange.initial`
  owns this choice.
* **"Since last visit" means the previous app run that opened the tab.** `WhatChangedVisitTracker`
  reads and replaces the stored visit once per process, so reopening the tab in the same run never
  moves the start. Never visited means all recorded history. There is no 7-day floor, so "No changes
  since …" is a valid summary.
* **Changed apps show the net change over the range** (state before the range vs now), so a
  permission added and removed again reads "No net change". Needs attention stays per event and marks
  an undone event with its revert date.
* Section rows can repeat an app across sections, so LazyColumn keys carry the section prefix. Locked
  rows keep the same key as their readable form; the key is never rendered. Rows and section headers
  use lazy-item animations for insertion, removal, and movement as history arrives; never force
  scrolling to new entries or enable these animations globally in the shared list helper.

## Pro gate

Applies to this tab and its full log only. Per-app history in `feature:app-detail` stays free for
every app.

* **One decision per row, from one class.** `WhatChangedAccessPolicy` reads
  `EntitlementsRepository.entitlements()` and builds a `WhatChangedAccess`; both ViewModels ask it
  whether each row is readable.
* **Without Pro, previews are positional:** the first five rows in each tab section and the first
  ten rows in the full log are readable, in their existing newest-first order. A readable row never
  unlocks later rows for the same app. The log assigns access before category filtering and month
  grouping, so filters cannot reveal additional rows. Also free: any certificate change (and a
  Changed apps row whose net change includes one) and all first-recorded rows. These exceptions
  still occupy their normal positions in the preview. Summary counts and range chips stay free.
  Everything else is a locked row. With Pro (and in release, where `noop` grants it) nothing is locked.
* **A locked row never carries real data.** State holds `LockableRow.Locked(key)` only, and
  `LockedRow` draws blurred placeholder shapes with a `ProBadge`, because `Modifier.blur` does nothing
  below Android 12 and TalkBack reads text. Its only semantics are the "Locked app change, part of
  Pro" description and the click action. There is no long-press on a locked row.
* **Tapping a locked row opens the paywall only**, with `PaywallReason.WhatChanged` quoting the
  number of distinct locked apps. Nothing comes back from the sheet: rows unlock when
  `entitlements()` reports Pro.
* The log's filter chips still filter locked rows by their categories, so a locked row can appear
  under a filter it matches.
