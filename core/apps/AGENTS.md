# core:apps Module

## Purpose
Core domain module for app analysis. Provides repositories and utilities for querying installed apps, app details, storage/usage stats, and APK analysis.

## Package: `sk.styk.martin.apkanalyzer.core.apps`

## Structure

```
InstalledAppsRepository.kt / Impl    - Flow of all installed apps
AppDetailRepository.kt / Impl        - Full app detail (installed package or APK file)
AppSigningRepository.kt / Impl       - Flow<Map<packageName, AppSigning>> for every installed app, one
                                        bulk GET_SIGNING_CERTIFICATES query + CertificateExtractor per
                                        entry. Lazily shared (unlike InstalledAppsRepository's Eagerly)
                                        since the per-cert digest/verify work only matters once a real
                                        consumer subscribes
DeviceFeaturesRepository.kt / Impl   - What *this device* provides, for checking an app's requirements
StorageStatsRepository.kt / Impl     - App storage size stats (requires USAGE_STATS permission)
UsageStatsRepository.kt / Impl       - App usage time/frequency stats
PackageChangesObserver.kt / Impl     - BroadcastReceiver-based package install/uninstall listener
AppClassificationThresholds.kt      - Constants for "large app", "recently installed", "unused" thresholds
AppExportManager.kt / Impl          - SAF-backed base APK and full-resolution icon export
analysis/
  CertificateExtractor.kt / Impl    - APK signing certificate extraction
  ApkSigningBlockAnalyzer.kt / Impl - Verifies v1 signing and parses the raw APK Signing Block
                                       for v2/v3/v3.1 ID-value pairs
  ManifestParser.kt / Impl           - Installed/APK AndroidManifest.xml parsing into readable namespaced XML
                                       and component intent filters
  InstallSourceResolver.kt / Impl   - Determine app install source (Play Store, sideload, etc.)
  SdkVersionResolver.kt             - SDK version to Android name mapping
  AnalysisUtils.kt                   - Shared analysis helpers, incl. permission protection decoding
                                        and `readNativeLibraries()`, which opens the APK's `sourceDir`
                                        as a zip and reads `lib/<abi>/*.so` entries directly
model/
  InstalledApp.kt         - Basic installed app info (packageName, name, sizes, times, source,
                            targetSdk, minSdk, sharedUserId, category)
  AppCategory.kt          - `ApplicationInfo.category` int decoded to a domain enum; `Undefined` is
                            the untagged case (most third-party apps), not an error
  AppDetail.kt            - Complete app detail (info, permissions, activities, services, etc.)
  AppInfo.kt              - Core app metadata, including public manifest security flags from `ApplicationInfo`
  Permission.kt           - Single permission: name plus `details`, which is null when the device
                            can't resolve the permission (declaring app not installed)
  PermissionDetails.kt    - Resolved permission details; `protectionLevel` and `protectionFlags` are
                            domain enums, never raw `PermissionInfo` ints
  ProtectionLevel.kt      - Base protection level enum
  ProtectionFlag.kt       - Additional protection flags enum
  Permissions.kt          - Used + defined permissions container
  UsedPermission.kt       - Permission with grant status
  Activity.kt             - Activity component info. `isLauncher` is nullable: it is resolved from
                            `queryIntentActivities` for an installed package, and is `null` for an
                            APK file, where launcher resolution is unavailable — null means unknown,
                            never "not a launcher"
  Service.kt              - Service component info
  BroadcastReceiver.kt    - Receiver component info
  ContentProvider.kt      - Provider component info, incl. `ProviderPathPermission` (`<path-permission>`
                            entries) and the `ProviderPathMatchType` enum for `PatternMatcher`'s int type
  Certificate.kt          - Certificate details
  CertificatePrincipal.kt - Issuer/subject info
  CertificateTrustLevel.kt - Trust classification enum
  SignatureAlgorithmAssessment.kt - Signing digest security assessment
  AppSigning.kt           - Current certificates and verified signing-key history, plus
                            `hasMultipleSigners` (free from `SigningInfo`) and the nullable
                            `signingSchemeVersions` (requires parsing the APK Signing Block; null
                            when it can't be read confidently)
  SigningSchemeVersion.kt - v1 (legacy JAR signing) / v2 / v3 / v3.1 detected from the APK itself
  Feature.kt              - Sealed: `Hardware(name)` for a `uses-feature` name, `OpenGlEs(reqGlEsVersion)`
                            for a GL ES version requirement. A GL ES `FeatureInfo` carries a null
                            `name` and a `reqGlEsVersion`, so the two are different kinds of fact and
                            must not be collapsed into one string field
  DeviceFeatures.kt       - The device side of that comparison: available feature names plus the
                            device's GL ES version. `supports()` returns `null` for unknown, never
                            `false` — an unreadable package manager must not read as "missing"
  NativeLibraries.kt      - ABI folders and distinct `.so` filenames an APK actually ships, read from
                            its zip entries rather than trusted from `primaryCpuAbi`/`secondaryCpuAbi`,
                            which describe what the device picked, not what the APK contains
  InstallLocation.kt      - Install location enum
di/                       - Hilt module bindings
```

## Key Interfaces

- `InstalledAppsRepository.apps(): Flow<List<InstalledApp>>` - Live list of all installed apps
- `AppDetailRepository.details(reference: AppReference): Result<AppDetail>` - Full installed-package
  or APK-file details
- `DeviceFeaturesRepository.deviceFeatures(): DeviceFeatures` - Memoized once per process from a single
  `getSystemAvailableFeatures()` call. Device features cannot change without a reboot, so there is no
  flow, no observer, and no invalidation. Never call `hasSystemFeature` in a loop instead — that is N
  binder calls for an answer one call already gave
- `ManifestParser.manifest(reference: AppReference): Result<ParsedManifest>` - Readable manifest.
  Installed packages parse the base path directly and report additional split count; opening
  `AndroidManifest.xml` from merged resources can resolve to an arbitrary split.
- `AppExportManager` - Writes an APK or natural-resolution icon for an `AppReference` to a
  user-selected document URI
- `StorageStatsRepository.isPermissionGranted: StateFlow<Boolean>` - Usage access permission state
- `UsageStatsRepository.isPermissionGranted: StateFlow<Boolean>` - Usage stats permission state

## Signing Certificate Semantics

- `SigningInfo.apkContentsSigners` contains the current signer set. Multiple current signers are one
  package identity and cannot use signing-key rotation.
- For a single signer, `SigningInfo.signingCertificateHistory` is Android's verified rotation
  lineage in oldest-to-current order; its final entry is the current certificate. Keep current and
  past certificates as separate lists in `AppSigning` rather than attaching role flags to
  `Certificate`.
- Historical certificates identify keys previously trusted for the package. Do not claim they can
  sign normal updates; update and rollback capabilities depend on Android's lineage capabilities.
- Preserve X.509 validity as `Instant`. Convert to local dates only for display, and compare the
  exact instants when determining validity.
- Equal issuer and subject names mean self-issued, not necessarily self-signed. Label a certificate
  self-signed only after its signature verifies with its own public key.
- Assess recognized signature algorithms in the certificate domain model. The assessment covers
  the digest only; unsupported names remain unknown, and overall security also depends on key type
  and size. Feature ViewModels may map the typed result but must not duplicate its policy.
- `PackageManager`'s `SigningInfo` does not expose which signing scheme(s) an APK uses.
  `ApkSigningBlockAnalyzer` is an injected interface whose implementation reads the APK file directly
  (`applicationInfo.sourceDir`, which `AppDetailRepositoryImpl` already sets correctly for both
  installed packages and APK files). It verifies v1 by reading the signed manifest through
  `JarFile`'s verifier, then locates the APK Signing Block immediately before the ZIP End Of Central
  Directory record and reads its known v2/v3/v3.1 ID-value pairs. Every step is defensive: any
  structural surprise or verification failure yields `null` rather than a guessed version list, per
  this module's "never throw" rule. `AppDetailRepositoryImpl` invokes it only in the single-app flow,
  not from the bulk `AppSigningRepositoryImpl` scan, so the extra file I/O is not paid for every
  installed app merely to populate the device-wide certificate index.

## Device Requirement Semantics

`AppDetail` describes an app; device availability describes a *pairing* of an app and a device. Keep
them apart: `AppDetailRepositoryImpl` caches `AppDetail` keyed by package and invalidated by
`PackageChangesObserver`, so folding device state into it would make cached entries depend on a
second input the key does not mention and the invalidation does not track. Consumers inject both
repositories and combine them while mapping to state.

`NativeLibraries` follows the same split: it holds only what the APK ships (ABIs, `.so` filenames).
`Build.SUPPORTED_ABIS` is read where it's consumed (see `GeneralInfoViewModel`), not folded into the
cached model — it's a plain static field, not an expensive call, so it doesn't warrant a repository
the way `DeviceFeaturesRepository` does for `getSystemAvailableFeatures()`.

## Component Intent Filters

- `PackageInfo` exposes component metadata but not its declared intent filters.
- `queryIntentActivities` and the other `queryIntent*` APIs only return filters that match an intent
  the caller already knows, so they cannot enumerate an app's entry points.
- `ManifestParser.componentIntentFilters()` reads the base manifest and every installed split manifest
  with Android's public binary XML resource parser. It normalizes relative component class names before
  joining filters back to `PackageInfo` components. Component kind is part of the key because Android
  permits different component types to share the same class name.
- Every filter preserves its actions, categories, data rules, priority, order, and link-verification
  request. Multiple `<data>` tags accumulate as rules on one filter, matching Android's semantics.
  `host` and `port` are the one exception: Android pairs them per `<data>` tag into a single
  authority match, so they are combined into one `Host` rule (`host:port`) instead of two
  independently flattened rules — otherwise two `<data>` tags with different host/port combinations
  would render as an ambiguous cross-product. A `port` without a `host` is dropped, matching
  Android, which ignores it.
- Reading every installed split manifest is required, not best-effort: if any split manifest cannot
  be read, `componentIntentFilters()` fails for the whole package rather than silently returning
  filters from only the splits it could read. Partial results would understate a component's real
  entry points and read as "no intent filters" to callers that only check `Result.isSuccess`.

## External Entry Semantics

- These rules support the Components screen's technical filter. They are not a risk verdict and do
  not feed the hub — that needs a deliberate rule set, not just accurate raw data.
- Exported alone is not a finding. Launcher activities are expected to be exported and are excluded.
- An activity is unguarded only when launcher status is known to be false and no permission is required.
- Services and receivers are unguarded when exported without a required permission.
- An exported provider is unguarded when either its top-level read or write permission is missing, or
  any `<path-permission>` entry opens a path without one — see Content Provider Path Permissions.
- APK activities have unknown launcher status. The technical `Unprotected` filter includes exported
  activities without permission guards and may therefore include the launcher activity; the hub
  does not interpret this as a finding.

## Content Provider Path Permissions

- `ProviderInfo.pathPermissions` is populated by `PackageManager` whenever `GET_PROVIDERS` is
  queried — unlike intent filters, no manifest parsing is needed, and the flag was already in use.
  `AnalysisUtils.resolvePathPermissions()` maps it to `ProviderPathPermission`, and `PathPermission`'s
  `type` int becomes the typed `ProviderPathMatchType` enum.
- A `<path-permission>` can carve out access that the provider's top-level `readPermission` /
  `writePermission` does not grant: an entry with neither permission set opens that specific path
  regardless of what the provider otherwise requires. `ContentProvider.hasUnguardedPathPermission`
  captures exactly that unambiguous case — it does not attempt to resolve full path-matching
  precedence when multiple `<path-permission>` entries could overlap.
- `isExternallyReachableWithoutPermission` folds this in: a provider is reachable without permission
  if the top level is open **or** any path permission is. It deliberately does not flip the other
  way — a blank top-level permission still leaves any path not covered by a `<path-permission>` open,
  so path permissions can only add exposure to the verdict, never resolve it into "guarded."

## Dependencies
- `api(projects.core.common)` - exposes common models and DispatcherProvider
