# core:app-index Module

## Purpose
Builds `attribute → apps` indexes across every installed app — target SDK, min SDK, install source,
permission, shared UID, app category, and signing certificate (fingerprint, organization, country) —
for the `feature:browse` "Browse by Attribute" screen (roadmap `CE-01`). A pure bucketing layer: it
holds no `PackageManager` access of its own and reaches only two public `core:apps` repositories
(`InstalledAppsRepository`, `AppSigningRepository`), never `analysis/` internals
(`CertificateExtractor`, `InstallSourceResolver`) directly.

## Package: `sk.styk.martin.apkanalyzer.core.appindex`

## Structure

```
AppIndexRepository.kt / Impl  - Flow of AppIndexStatus; Impl combines InstalledAppsRepository.apps()
                                and AppSigningRepository.signing(), and holds the
                                (List<InstalledApp>, Map<packageName, AppSigning>) -> AppAttributeIndex
                                transform as private functions (one groupBy helper per dimension) —
                                inlined rather than a separate object since it has exactly one caller
model/
  AppAttributeIndex.kt         - targetSdk / minSdk / installSource / permission / certificateFingerprint / certificateOrganization / certificateCountry / sharedUserId / appCategory buckets, each Map<value, List<packageName>>
  AppIndexStatus.kt            - sealed: Loading | Data(index) - Loading before the first combined emission
di/
  AppIndexModule.kt            - Hilt @Binds for AppIndexRepository
```

## Key Interface

```kotlin
interface AppIndexRepository {
    fun index(): Flow<AppIndexStatus>
}
```

Recomputes whenever `InstalledAppsRepository.apps()` or `AppSigningRepository.signing()` emits — no
separate `PackageChangesObserver` subscription. Grouping is CPU-bound, not I/O, so the combine step
runs on `dispatcherProvider.default()` — the certificate digest/verify work itself happens upstream in
`AppSigningRepositoryImpl`, on `dispatcherProvider.io()`. The combined result is `shareIn(appScope,
SharingStarted.Lazily, replay = 1)`, matching `AppSigningRepositoryImpl`'s reasoning: the grouping work
only matters once a real consumer (a browse screen) subscribes, and multiple simultaneous subscribers
must not each redo it.

## Dimensions, and why the first six are cheaper than the last three

`targetSdk`, `minSdk`, `installSource`, `permission`, `sharedUserId`, `appCategory` are all already on
`InstalledApp` — no new query. `sharedUserId` is the one dimension that legitimately drops apps: only
those declaring `android:sharedUserId` are indexed (`bySharedUserId` `mapNotNull`s the null case away),
matching `byPermission`'s existing "no signal, no bucket" shape rather than inventing an "unshared"
sentinel bucket that would just be most of the device. `appCategory` keeps `AppCategory.Undefined` as
a real, groupable bucket instead, because most third-party apps genuinely declare no category and
that absence is itself the fact worth counting.
`certificateFingerprint`/`certificateOrganization`/`certificateCountry` come from
`AppSigningRepository`, which runs a real X.509 parse, six digest computations, and a signature-verify
per certificate — genuinely heavier, which is why that repository is `Lazily` shared (see
`core/apps/AGENTS.md`) rather than computed unconditionally like `InstalledAppsRepository`. A
multi-signer app is indexed under every current signer, not an arbitrary "first" one
(`byCertificate` `flatMap`s over `AppSigning.currentCertificates`). Fingerprint uses
`Certificate.certificateHashSha256` per roadmap `FR-09` ("same publisher" — group by fingerprint, not
signing algorithm); organization/country come from `Certificate.subject`, matching the certificate
screen's existing convention for "the signer."

**Uses-feature grouping is still deferred** — it would need a new `PackageManager` flag
(`GET_CONFIGURATIONS`) nothing currently requests. Unlike certificate, no other `core:apps` consumer
needs a device-wide feature list today, so there's no general-purpose repository to build yet; add one
the same way `AppSigningRepository` was added — in `core:apps`, not here — when a real second consumer
appears.

## Dependencies
- `implementation(projects.core.apps)` - `InstalledAppsRepository`, `AppSigningRepository`, and their
  models, public surface only
- `implementation(projects.core.common)` - `AppSource`, `DispatcherProvider`
