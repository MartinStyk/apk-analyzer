# feature:pro Module

## Purpose

The Apk Analyzer Pro paywall: one bottom sheet that sells the one-time `Entitlement.Pro` from
[`core:entitlements`](../../core/entitlements/AGENTS.md). Phone only; there is no TV module (TV is
out of Pro v1).

The API module exposes `ProNavKey(reason)`. `PaywallReason` says which locked feature opened the
sheet and carries the count its headline quotes (`Export(appCount)`, `WhatChanged(lockedAppCount)`).
The phone implementation uses the package `sk.styk.martin.apkanalyzer.feature.pro.phone`.

## Package Map

* Root - `ProScreen` (the sheet content) and `ProViewModel` with State/Action/Event.
* `analytics/` - the paywall funnel: opened, purchase started and finished, restore finished, each
  with the paywall source.
* `navigation/` - the entry provider. The entry carries `BottomSheetSceneStrategy.bottomSheet()`, so
  it shows as a sheet over the screen that opened it.

## Rules

* **Open the sheet only from an explicit tap on something locked.** Never on launch, on a timer, or
  after a number of uses.
* **The sheet returns nothing to its origin.** Origins read `EntitlementsRepository.entitlements()`
  and unlock when it holds `Entitlement.Pro`; no navigation result is passed back.
* The sheet closes itself once Pro is owned: after a purchase, after a restore that finds Pro, or
  when it opens for someone who already owns it. It closes once, so it never pops the origin.
* `purchase()` needs an `Activity`; the screen passes `LocalActivity` in `ProAction.Unlock`, and the
  ViewModel never stores it.
* No offer means Play cannot sell here (no Play services, sideloaded build, or the release `noop`
  binding): the sheet says purchases need Google Play and shows no buy or restore action.
* Copy states counted facts only: no urgency, timers or superlatives. The price comes from Play.
