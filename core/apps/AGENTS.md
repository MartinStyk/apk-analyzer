# core:apps Module

## Purpose
Core domain module for app analysis. Provides repositories and utilities for querying installed apps, app details, storage/usage stats, and APK analysis.

## Package: `sk.styk.martin.apkanalyzer.core.apps`

## Structure

```
InstalledAppsRepository.kt / Impl    - Flow of all installed apps
AppDetailRepository.kt / Impl        - Full app detail (installed package or APK file)
StorageStatsRepository.kt / Impl     - App storage size stats (requires USAGE_STATS permission)
UsageStatsRepository.kt / Impl       - App usage time/frequency stats
PackageChangesObserver.kt / Impl     - BroadcastReceiver-based package install/uninstall listener
AppClassificationThresholds.kt      - Constants for "large app", "recently installed", "unused" thresholds
analysis/
  CertificateExtractor.kt / Impl    - APK signing certificate extraction
  ManifestParser.kt                  - AndroidManifest.xml parsing
  InstallSourceResolver.kt / Impl   - Determine app install source (Play Store, sideload, etc.)
  SdkVersionResolver.kt             - SDK version to Android name mapping
  AnalysisUtils.kt                   - Shared analysis helpers, incl. permission protection decoding
model/
  InstalledApp.kt         - Basic installed app info (packageName, name, sizes, times, source)
  AppDetail.kt            - Complete app detail (info, permissions, activities, services, etc.)
  AppInfo.kt              - Core app metadata
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
  Feature.kt              - Hardware/software feature
  InstallLocation.kt      - Install location enum
di/                       - Hilt module bindings
```

## Key Interfaces

- `InstalledAppsRepository.apps(): Flow<List<InstalledApp>>` - Live list of all installed apps
- `AppDetailRepository.installedPackageDetails(packageName: String): Result<AppDetail>` - Full details for installed app
- `AppDetailRepository.apkFilePackageDetails(file: File): Result<AppDetail>` - Full details for APK file
- `StorageStatsRepository.isPermissionGranted: StateFlow<Boolean>` - Usage access permission state
- `UsageStatsRepository.isPermissionGranted: StateFlow<Boolean>` - Usage stats permission state

## Dependencies
- `api(projects.core.common)` - exposes common models and DispatcherProvider

