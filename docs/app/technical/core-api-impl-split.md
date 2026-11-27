# Core API/Impl Split and Entitlements

**Status:** Implemented for the existing core domain modules (`apps`, `app-permissions`,
`app-index`, `app-history`, `app-export`, `apk-files`, `ai-insights`, `user-preferences`), with the
convention plugins and `validateArchitecture` checks, and for the new `core:entitlements` with its
Google Play Billing `impl` and `noop` implementations.
**Scope:** How a core domain module splits into an `api` and an implementation module, where models,
resources, and top-level functions go, how the split is enforced, and how `core:entitlements` swaps
its Google Play Billing implementation for a no-op one per build type. Does not cover the
entitlement product design: what is paid, pricing, or the purchase UI.

## Why

* Google Play Billing has to be optional. Release builds ship without it for now, so every consumer
  of entitlements must compile against a contract and never against the billing implementation.
* Before the split the only boundary inside a core module was `internal`. The split makes it
  structural: a feature cannot reach an implementation class because the module isn't on its
  classpath.
* It mirrors `feature/*/api`, which already lets features depend on each other's contract only.

## Module Shape

| Module | Holds |
|---|---|
| `core/<name>/api` | Public interfaces, models, constants, pure extensions on those models, model labels |
| `core/<name>/impl` | The implementation: `Impl` classes, Hilt bindings, workers, Room, platform access |

A domain with a second implementation keeps `impl` for the real one and names the other after what it\nis:

| Module | Holds |
|---|---|
| `core/entitlements/api` | `EntitlementsRepository` and the entitlement models |
| `core/entitlements/impl` | The Google Play Billing implementation; the only module with the billing library |
| `core/entitlements/noop` | Grants every entitlement and offers nothing for sale |

Packages and namespaces follow the existing directory rule: `core.entitlements.api`,
`core.entitlements.noop`, `core.apps.api`, `core.apps.impl`. Each module therefore has its own `R`
class.

### Which modules split

Domain modules with a contract that features consume split: `apps`, `app-permissions`, `app-index`,
`app-history`, `app-export`, `apk-files`, `ai-insights`, `user-preferences`, and the new
`entitlements`.

These stay single modules:

* `design-system` — a UI toolkit, not a domain. There is no second implementation of a button.
* `common` — dispatchers, logging, and shared primitives at the bottom of the graph.
* `navigation` — infrastructure the app shells and feature entry providers call directly.
* `app-functions` — only `app` depends on it. It is already an implementation; it depends on other
  modules' `api`.

## What Goes Where

### `api`

* Public interfaces for repositories and managers. Interface methods still never throw.
* Models: `@Serializable` data classes, sealed interfaces, enums.
* Constants consumers need, in an `object`.
* Pure top-level extensions on `api` models that need no dependency, no `Context`, and could never
  have a second implementation — they define what the model means. Examples in `core:apps`:
  `isLaunchable`, `hasUnguardedPathPermission`, `isExternallyReachableWithoutPermission`.
* `testFixtures` with a fake for every interface, so feature and domain tests never depend on an
  implementation module.

`api` never contains Hilt modules, `@Inject` classes, Compose, or platform access. It is still an
Android library, because it may own resources (see below).

### Implementation

* The `Impl` classes, the `@Module @InstallIn(SingletonComponent::class)` binding interface, workers,
  Room databases, `PackageManager` and other platform access.
* Logic that is injected — evaluators, mappers, policies — is an interface in `api` and a class in the
  implementation. The "logic lives in classes, not objects" rule is unchanged; the class just moves
  behind an interface.
* **Everything is `internal`.** Nothing outside the app shells ever sees this module, so a public
  declaration here is always a mistake.
* Unit tests for the implementation live here.

### Resources

A string lives in the lowest module that needs it:

* **Labels and explanations for `api` models go in `api`**, together with their `@StringRes`
  extensions. `ComponentFlag.labelRes`, `exportedExplanationRes`, and
  `RequirementItem.availabilityExplanationRes()` in `core:apps` are this kind. Both implementations and every feature share them, so they are part of
  the contract.
* **Strings only the implementation uses go in the implementation module**, such as notification
  titles or worker channel names.
* **Feature copy lives in the feature, not in a core module.** A string only one feature shows goes
  to that feature: its `domain` module when phone and TV both show it (as `feature:app-detail` and
  `feature:browse` do), its `phone` or `tv` module otherwise. A string several features or a core
  module show stays with the `api` model it labels.

Moving a string between modules moves it in every `values-<locale>/strings.xml` as well; the
`translate-strings` skill covers keeping locale key sets aligned.

### Top-level functions and objects

| Shape | Goes in |
|---|---|
| Pure extension or predicate on `api` models | `api` |
| `@StringRes` mapping for an `api` model | `api`, with the strings |
| Constants a consumer needs | `api` |
| Anything needing `Context`, `PackageManager`, I/O, or a dependency | implementation, behind an `api` interface |
| Constants only the implementation uses | implementation, `internal` |

## Dependency Rules

```mermaid
graph TD
    app[":app / :tv"]
    feature["feature/*/phone, tv, domain"]
    api["core/*/api"]
    impl["core/*/impl, noop"]
    common["core:common"]

    app --> impl
    app --> api
    feature --> api
    impl --> api
    api --> api
    api --> common
    impl --> common
```

* An `api` module depends only on other `core/*/api` modules and `core:common`.
* An implementation depends on its own `api` and on other modules' `api`, never on another
  implementation.
* Only `app`, `tv`, and `app:baselineprofile` depend on implementation modules.
* Features and feature `domain` modules depend on `api` only.

## Entitlements Wiring

The app shells pick the implementation per build type. Debug ships Google Play Billing, release
ships the no-op:

```kotlin
dependencies {
    implementation(projects.core.entitlements.api)
    debugImplementation(projects.core.entitlements.impl)
    releaseImplementation(projects.core.entitlements.noop)
}
```

`tv` uses the same three lines. Switching release to Play Billing later is a one-line change in each
shell.

* Hilt fails to compile a variant with no `EntitlementsRepository` binding, so a missing or duplicate
  implementation cannot ship silently.
* The baseline profile plugin adds its own `nonMinifiedRelease` and `benchmarkRelease` build types.
  They inherit `releaseImplementation`, so both resolve the no-op without a declaration of their own.
* `noop` grants every entitlement and exposes no offers. Purchase UI derives from the offers, so it
  disappears on its own in release without a separate "billing enabled" flag.
* `impl` maps every billing failure, including `BILLING_UNAVAILABLE` on a sideloaded debug build,
  to an empty set, an empty list, or `PurchaseResult.Failed`. It never throws, following the
  existing interface rule.

### Contract

The product design decides the actual entitlements. Until then there is one, `Entitlement.Pro`, a
one-time in-app product whose Play product ID is a placeholder in `impl`.

```kotlin
interface EntitlementsRepository {
    fun entitlements(): Flow<Set<Entitlement>>
    fun offers(): Flow<List<Offer>>
    suspend fun purchase(activity: Activity, offer: Offer): PurchaseResult
}
```

* `entitlements()` is what the user currently has access to, so a paid feature checks access from a
  single source rather than each screen asking billing on its own.
* `offers()` is what can still be bought: owned entitlements are excluded, and it is empty in `noop`.
* `purchase` takes the `Activity` Play Billing needs to launch its flow, and reports `Purchased`,
  `Pending`, `Cancelled`, or `Failed`. `android.app.Activity` is allowed in `api` for it (see
  Enforcement).

## Enforcement

* **Convention plugins.** Built. `apkanalyzer.core.api` configures an Android library with the
  coroutines and serialization runtimes and `testFixtures`, and no Hilt or Compose. An `api` module
  with `@StringRes` labels adds `androidx-core-ktx` for the annotation. `apkanalyzer.core.impl` adds
  Hilt. Each module applies one or the other, never both.
* **`validateArchitecture`.** Built: A7 keeps an `api` module on `api` modules and `core:common`, A8
  keeps implementation modules off everything but the app shells, and B6/B7 check which convention
  each side applies, so an `api` module never gets Hilt.
* **The `api` classpath and `ForbiddenImport/CoreApiPlatformAccess`.** Hilt, Room, WorkManager, and
  Compose are not on an `api` module's classpath, so the compiler rejects them. The Android framework
  always is, so a second, path-scoped instance of detekt's `ForbiddenImport` rejects every `android.*`
  and `androidx.*` import in `api` main and test-fixture sources except `android.net.Uri` (a value in
  contracts), `android.app.Activity` (the host Play Billing's purchase flow needs, passed through),
  `android.content.res.Resources` (resolving labels), and `androidx.annotation.*`.
  Mapping a platform value such as a `PackageInfo` flag to an `api` model belongs in `impl`.
* **A detekt rule in `build-logic/detekt-rules`.** Built: `ImplModuleVisibility` reports any public
  top-level declaration in a nested core module other than `api` and `design-system`, so every new
  implementation module (`impl`, `noop`) is covered without a config change.
* **`core/<name>/AGENTS.md`** stays one file per domain, covering both submodules, the same way
  `feature/<name>/AGENTS.md` and `core/design-system/AGENTS.md` already cover theirs.

## Migration

1. Done: the convention plugins and the `validateArchitecture` and detekt checks.
2. Done: the existing modules, in dependency order (`apk-files`, `apps`, `app-permissions`,
   `app-index`, `user-preferences`, `ai-insights`, `app-export`, `app-history`), so each module's
   `api` only ever depended on modules that had already split.
3. Done: `core:entitlements`, built with this shape. It was new, so nothing moved.

Migrating one module means: create `api`, move interfaces, models, labels, and pure extensions into
it, rename the remaining module to `impl` and make it `internal`, repoint consumers' imports and
dependencies to `api`, and add the `impl` dependency to the app shells that use it (TV ships
neither `ai-insights` nor `app-export`).

Moving a class to a new package is not free for anything Android stores by name. Room's exported
schema directory is named after the database class and moves with it. WorkManager persists the
worker class name, so `core:app-history` enqueues its periodic worker with
`ExistingPeriodicWorkPolicy.UPDATE`, which rewrites already-enqueued work to the new class. The
backup agent is named in the app manifest. Polymorphic serialized blobs use explicit `@SerialName`
values, so they are unaffected.

## Open Questions

* **Purchase handoff.** `purchase` takes the `Activity`, so no ViewModel needs to hold one. The
  likely caller is a ViewModel event the screen handles, calling the repository with the `Activity`
  it already has; confirm when the purchase UI is designed.
* **Products.** Which features `Pro` unlocks, its price, and its real Play product ID replace the
  placeholder once the product design is decided.
* **A billing-free public build.** If a build without billing is ever distributed, for example on
  F-Droid, build types stop being enough and the app shells move to product flavors. Nothing in the
  module shape changes; only the wiring does.
