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
  ManifestParser.kt / Impl           - Installed/APK AndroidManifest.xml parsing into readable namespaced XML
  InstallSourceResolver.kt / Impl   - Determine app install source (Play Store, sideload, etc.)
  SdkVersionResolver.kt             - SDK version to Android name mapping
  AnalysisUtils.kt                   - Shared analysis helpers, incl. permission protection decoding
model/
  InstalledApp.kt         - Basic installed app info (packageName, name, sizes, times, source,
                            targetSdk, minSdk)
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
                            APK file, where the intent filters cannot be read — null means unknown,
                            never "not a launcher"
  Service.kt              - Service component info
  BroadcastReceiver.kt    - Receiver component info
  ContentProvider.kt      - Provider component info
  Certificate.kt          - Certificate details
  CertificatePrincipal.kt - Issuer/subject info
  CertificateTrustLevel.kt - Trust classification enum
  SignatureAlgorithmAssessment.kt - Signing digest security assessment
  AppSigning.kt           - Current certificates and verified signing-key history
  Feature.kt              - Sealed: `Hardware(name)` for a `uses-feature` name, `OpenGlEs(reqGlEsVersion)`
                            for a GL ES version requirement. A GL ES `FeatureInfo` carries a null
                            `name` and a `reqGlEsVersion`, so the two are different kinds of fact and
                            must not be collapsed into one string field
  DeviceFeatures.kt       - The device side of that comparison: available feature names plus the
                            device's GL ES version. `supports()` returns `null` for unknown, never
                            `false` — an unreadable package manager must not read as "missing"
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

## Device Requirement Semantics

`AppDetail` describes an app; device availability describes a *pairing* of an app and a device. Keep
them apart: `AppDetailRepositoryImpl` caches `AppDetail` keyed by package and invalidated by
`PackageChangesObserver`, so folding device state into it would make cached entries depend on a
second input the key does not mention and the invalidation does not track. Consumers inject both
repositories and combine them while mapping to state.

## External Entry Semantics

- These rules support the Components screen's technical filter. They are not a risk verdict and do
  not feed the hub until intent filters and path permissions provide the missing context.
- Exported alone is not a finding. Launcher activities are expected to be exported and are excluded.
- An activity is unguarded only when launcher status is known to be false and no permission is required.
- Services and receivers are unguarded when exported without a required permission.
- An exported provider is unguarded when either its read or write path lacks a permission.
- APK activities have unknown launcher status. The technical `Unprotected` filter includes exported
  activities without permission guards and may therefore include the launcher activity; the hub
  does not interpret this as a finding.

## Dependencies
- `api(projects.core.common)` - exposes common models and DispatcherProvider
