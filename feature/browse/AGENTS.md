# feature:browse Module

## Purpose
Bottom-navigation top-level destination for "Browse by Attribute" (tab label "Browse") — roadmap
`CE-05`: pick a dimension, see its bucket counts, tap a bucket to see the apps in it. Three depths,
one screen each: hub (dimension cards) → options (bucket list for the chosen dimension) → apps
(the apps in the tapped bucket) → `feature:app-detail`. Replaces the former `feature:permissions` and
`feature:statistics` stub tabs, per `docs/product/roadmap.md` §1.1b's module consolidation note.

## Sub-modules
- `api` — Contains `BrowseNavKey` (top-level destination) and the "Browse" string resource used as
  the bottom-nav tab label
- `impl` — Full implementation

## Package: `sk.styk.martin.apkanalyzer.feature.browse.impl`

## API Module Key Types

```kotlin
@Serializable
object BrowseNavKey : NavKey
```

## Impl Structure

```
model/
  BrowseDimension.kt        - @Serializable enum: Permission, SigningCertificate, TargetSdk, MinSdk, InstallSource
domain/
  BrowseBuckets.kt           - AppAttributeIndex.bucketsFor(dimension, subAttribute):
                                Map<String, List<PackageName>>, normalizing every dimension's key type
                                (Int, AppSource, String?) to String. Certificate identity can use
                                SHA-256, SHA-1, or MD5. UNKNOWN_SIGNER_KEY is the sentinel for a null
                                certificate organization
  BrowseDimensionLabeler.kt  - @Inject; resolves a dimension+key pair to a friendly label and an
                                optional raw identifier. Wraps PermissionLabelProvider (core:app-permissions)
                                and SdkVersionResolver (core:apps) for the two dimensions that need a
                                real lookup; the enum-backed dimensions (install source, unknown signer)
                                resolve directly from this module's own strings.xml via Context.getString
                                — deliberately not through Compose stringResource, since this runs outside
                                a @Composable in the ViewModel/domain layer
BrowseDimensionResources.kt  - Composable-only per-dimension icon/title/subtitle, shared by all three screens
BrowseState.kt / BrowseAction.kt / BrowseEvent.kt / BrowseViewModel.kt / BrowseScreen.kt
                              - Hub: dimension cards with a top-labels preview row and an option count,
                                sourced straight from AppIndexRepository.index() + InstalledAppsRepository.apps()
options/                      - Shared bucket-list shell (search + sorted-by-count rows) with typed
                                option models and dimension-specific row content; certificate hash
                                rows show the complete fingerprint in a monospaced HashBox
apps/                         - Apps in one tapped bucket (search + app rows), re-derives the bucket's
                                package set live from the index rather than carrying it through the NavKey
navigation/
  BrowseEntryProvider.kt      - browseEntries(navigator): registers BrowseNavKey, BrowseOptionsNavKey,
                                BrowseAppsNavKey; the apps screen navigates out to AppDetailNavKey
  BrowseOptionsNavKey.kt      - Internal: dimension
  BrowseAppsNavKey.kt         - Internal: dimension, bucketKey, bucketLabel
```

None of the three screens have an error state — `AppIndexStatus` is `Loading | Data`, never a failure,
since it is pure bucketing over flows that already can't fail. Loading is the only non-loaded state
each ViewModel exposes.

The apps screen does not carry the bucket's package list through the NavKey. It re-combines
`AppIndexRepository.index()` with `InstalledAppsRepository.apps()` using the dimension and raw bucket
key alone, so the list stays live (uninstalling an app while viewing its bucket updates the screen)
and the NavKey stays small.

## Dependencies
- `api`: `apkanalyzer.feature.api` plugin, no explicit dependencies
- `impl`: `apkanalyzer.feature.impl` plugin, `api(projects.feature.browse.api)`,
  `implementation(projects.feature.appDetail.api)`, `implementation(projects.core.appIndex)`,
  `implementation(projects.core.apps)`, `implementation(projects.core.appPermissions)`,
  `implementation(projects.core.common)`, `kotlinx-collections-immutable`
