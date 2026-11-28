# feature:pro Module

## Purpose

The Apk Analyzer Pro paywall: one bottom sheet that sells the one-time `Entitlement.Pro` from
[`core:entitlements`](../../core/entitlements/AGENTS.md). Phone only; there is no TV module (TV is
out of Pro v1).

The API module exposes `ProNavKey(reason)`. `PaywallReason` says what opened the sheet:
`Export` (with the app count, sample app names and the free and Pro detail counts its picture and
copy quote), `WhatChanged(placement)` (row, card, bar or log, for analytics), `Settings`, or
`Launch` (the after-update support prompt). The design is
[`docs/app/product/features/pro.md`](../../docs/app/product/features/pro.md).
The phone implementation uses the package `sk.styk.martin.apkanalyzer.feature.pro.phone`.

## Package Map

* Root - `ProScreen` (the sheet content) and `ProViewModel` with State/Action/Event. The sheet reads
  the history counts it quotes from `AppHistoryProSummary` in `core:app-history`, the same rule that
  locks rows in What Changed.
* `support/` - the after-update support prompt: `SupportPromptPolicy` (when it may show),
  `SupportPromptScheduler` (collected by the app shell, which opens `PaywallReason.Launch`), the
  session flag recording that a paywall was already shown, and install details.
* `analytics/` - the paywall funnel: opened, purchase started and finished, restore finished, each
  with the paywall source.
* `navigation/` - the entry provider. The entry carries `BottomSheetSceneStrategy.bottomSheet()`, so
  it shows as a sheet over the screen that opened it.

## Rules

* **Open the sheet from an explicit tap on something locked or on the Settings Pro row.** The one
  exception is the after-update support prompt, which `SupportPromptPolicy` allows at most twice ever,
  only after an accomplishment in the current run and never on cold start. Never on a timer or after a
  number of uses.
* **The sheet returns nothing to its origin, with one exception.** Origins read
  `EntitlementsRepository.entitlements()` and unlock when it holds `Entitlement.Pro`. The exception is
  `ProSheetResult.ExportBasicsOnly`, sent with `Navigator.goBack(result)` so the export screen
  switches to the Basics preset.
* The sheet closes itself once Pro is owned: after a purchase, after a restore that finds Pro, or
  when it opens for someone who already owns it. It closes once, so it never pops the origin.
* `purchase()` needs an `Activity`; the screen passes `LocalActivity` in `ProAction.Unlock`, and the
  ViewModel never stores it.
* No offer means Play cannot sell here (no Play services, sideloaded build, or the release `noop`
  binding): the sheet says purchases need Google Play and shows no buy or restore action.
* Copy states counted facts only: no urgency, timers or superlatives. The price comes from Play.
