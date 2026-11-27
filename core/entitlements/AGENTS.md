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

Pricing, which features are paid, and the purchase UI are not decided yet. `Entitlement.Pro` is a
one-time in-app product whose Play product ID is the placeholder `todo` in `impl`; replace it once the
product exists in Play Console.

## Contract

* `entitlements()` is the single source of what the user has. A paid feature checks it rather than
  asking billing itself.
* `offers()` lists only what can still be bought, so it excludes owned entitlements. Purchase UI
  derives from it, which makes it disappear in `noop` without a separate "billing enabled" flag.
* `purchase(activity, offer)` takes the `Activity` Play Billing needs to show its flow. The screen
  calls it with the `Activity` it already has, from a ViewModel event, so no ViewModel holds one.
  `android.app.Activity` is allowed in `api` by `ForbiddenImport/CoreApiPlatformAccess` for this.
* Nothing throws. Every billing failure, including `BILLING_UNAVAILABLE` on a sideloaded debug build,
  becomes an empty set, an empty list, or `PurchaseResult.Failed`.

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
  It re-queries purchases on every update. Nothing refreshes on returning to the foreground yet; add
  that with the first screen that keeps a collector alive.
