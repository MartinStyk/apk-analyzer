# core:entitlements Module

## Purpose and Boundary

Owns what the user has paid for and what can be bought. It is split per
[core API/impl split](../../docs/app/technical/core-api-impl-split.md), and is the one core domain
with two implementations:

| Module | Package | Holds |
|---|---|---|
| `api` | `core.entitlements.api` | `EntitlementsRepository`, `Entitlement`, `Offer`, `PurchaseResult`, and `EntitlementsRepositoryFake` in `testFixtures` |
| `impl` | `core.entitlements.impl` | The Google Play Billing implementation. The only module with the billing library |
| `noop` | `core.entitlements.noop` | Grants every entitlement and offers nothing for sale |

The app shells pick one per build type: `debugImplementation(projects.core.entitlements.impl)` and
`releaseImplementation(projects.core.entitlements.noop)` in both `app` and `tv`. The baseline profile
build types (`nonMinifiedRelease`, `benchmarkRelease`) inherit the release dependencies, so they
resolve `noop` without their own declaration. Never depend on `impl` or `noop` from anything other
than the app shells.

Pricing and the purchase UI live in [`feature:pro`](../../feature/pro/AGENTS.md). `Entitlement.Pro`
is a one-time in-app product whose Play product ID is the placeholder `todo` in `impl`; replace it
once the product exists in Play Console.

## Contract

* `entitlements()` is the single source of what the user has. A paid feature checks it rather than
  asking billing itself.
* `offers()` lists only what can still be bought, so it excludes owned entitlements. Purchase UI
  derives from it, which makes it disappear in `noop` without a separate "billing enabled" flag.
* `purchase(activity, offer)` takes the `Activity` Play Billing needs to show its flow. The screen
  calls it with the `Activity` it already has, passed through a ViewModel action that never stores
  it.
  `android.app.Activity` is allowed in `api` by `ForbiddenImport/CoreApiPlatformAccess` for this.
* `refresh()` re-reads purchases from Play and returns what the user now owns as a `Result`, which
  fails when Play could not be reached. A successful result is handed to active `entitlements()` collectors
  directly, so they unlock without another Play query. "Restore purchase" calls it.
* Nothing throws. Every billing failure, including `BILLING_UNAVAILABLE` on a sideloaded debug build,
  becomes an empty set, an empty list, or `PurchaseResult.Failed`. The one exception: once an
  `entitlements()` collector has read what the user owns, a later failed query keeps that value
  instead of emitting an empty set, so a dropped Play connection never relocks Pro.

## Play Billing Behavior

* Only `PURCHASED` purchases grant an entitlement; `PENDING` ones do not until Play reports them
  purchased.
* Play refunds a purchase that isn't acknowledged within three days. Every entitlements query and
  every successful `purchase` acknowledges unacknowledged purchases, so a purchase that completes
  while the app is closed is still acknowledged the next time entitlements are read.
* `ITEM_ALREADY_OWNED` counts as a successful purchase. The purchase is queried (which acknowledges
  it) and active `entitlements()` collectors are told to refresh, so stale offers disappear.
* Each `entitlements()` collector subscribes to `PurchaseUpdates`, the `PurchasesUpdatedListener` the
  `BillingClient` is built with, before its first query, so an update during that query is not lost.
  It re-queries purchases on every update, and `ForegroundPurchaseRefresh` sends an update each time
  the app returns to the foreground, so a purchase completed or refunded outside the app shows up on
  return.
