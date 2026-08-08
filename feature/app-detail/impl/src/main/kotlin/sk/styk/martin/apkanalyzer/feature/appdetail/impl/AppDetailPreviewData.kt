package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.CertificatePrincipal
import sk.styk.martin.apkanalyzer.core.apps.model.CertificateTrustLevel
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import java.time.Instant

internal fun sampleLoadedState() = AppDetailState.Loaded(
    analysisMode = AppDetail.AnalysisMode.InstalledPackage,
    appName = "Spotify",
    packageName = PackageName("com.spotify.music"),
    processName = "com.spotify.music",
    versionName = "9.4.12",
    versionCode = 90412,
    uid = 10234,
    description = null,
    isSystemApp = false,
    source = "GooglePlay",
    apkDirectory = "/data/app/com.spotify.music/base.apk",
    dataDirectory = "/data/data/com.spotify.music",
    apkSize = 152.megabytes,
    totalSize = 510.megabytes,
    targetSdkVersion = 35,
    targetSdkLabel = "Android 15",
    minSdkVersion = 24,
    minSdkLabel = "Android 7.0",
    installLocation = "InternalOnly",
    appInstaller = PackageName("com.android.vending"),
    firstInstallTime = Instant.ofEpochMilli(1_736_640_000_000),
    lastUpdateTime = Instant.ofEpochMilli(1_748_736_000_000),
    totalPermissionsCount = 32,
    dangerousPermissionsCount = 6,
    grantedDangerousPermissionsCount = 4,
    dangerousPermissionPreviews = persistentListOf(
        AppDetailState.Loaded.PermissionPreview(
            name = "android.permission.CAMERA",
            groupName = "android.permission-group.CAMERA",
            label = "Camera",
        ),
        AppDetailState.Loaded.PermissionPreview(
            name = "android.permission.RECORD_AUDIO",
            groupName = "android.permission-group.MICROPHONE",
            label = "Microphone",
        ),
        AppDetailState.Loaded.PermissionPreview(
            name = "android.permission.ACCESS_FINE_LOCATION",
            groupName = "android.permission-group.LOCATION",
            label = "Location",
        ),
        AppDetailState.Loaded.PermissionPreview(
            name = "android.permission.READ_CONTACTS",
            groupName = "android.permission-group.CONTACTS",
            label = "Contacts",
        ),
        AppDetailState.Loaded.PermissionPreview(
            name = "android.permission.READ_MEDIA_AUDIO",
            groupName = "android.permission-group.READ_MEDIA_AURAL",
            label = "Music and audio",
        ),
        AppDetailState.Loaded.PermissionPreview(
            name = "android.permission.POST_NOTIFICATIONS",
            groupName = "android.permission-group.NOTIFICATIONS",
            label = "Notifications",
        ),
    ),
    definedPermissionsCount = 1,
    activitiesCount = 428,
    servicesCount = 57,
    contentProvidersCount = 4,
    broadcastReceiversCount = 89,
    certificatesCount = 2,
    requirementsCount = 12,
    requiredFeaturesCount = 9,
    optionalFeaturesCount = 3,
    unmetRequirementsCount = 1,
    requirementPreviews = persistentListOf(
        AppDetailState.Loaded.RequirementPreview(name = "android.hardware.nfc", isUnmetRequirement = true),
        AppDetailState.Loaded.RequirementPreview(name = "android.hardware.camera", isUnmetRequirement = false),
        AppDetailState.Loaded.RequirementPreview(name = "android.hardware.wifi", isUnmetRequirement = false),
        AppDetailState.Loaded.RequirementPreview(name = "android.hardware.bluetooth_le", isUnmetRequirement = false),
        AppDetailState.Loaded.RequirementPreview(name = null, isUnmetRequirement = false),
    ),
    certificate = AppDetailState.Loaded.CertificateState(
        signAlgorithm = "SHA256withRSA",
        sha256Fingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2",
        issuer = CertificatePrincipal(name = "Android", organization = "Google Inc."),
        trustLevel = CertificateTrustLevel.Valid,
    ),
    insights = persistentListOf(
        AppDetailInsight.Debuggable,
        AppDetailInsight.SensitivePermission(
            access = SensitiveAccess.BackgroundLocation,
            permissionName = "android.permission.ACCESS_BACKGROUND_LOCATION",
        ),
    ),
)
