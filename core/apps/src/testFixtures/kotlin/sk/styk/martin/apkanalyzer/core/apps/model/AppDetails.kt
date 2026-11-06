package sk.styk.martin.apkanalyzer.core.apps.model

import sk.styk.martin.apkanalyzer.core.apps.components.Activity
import sk.styk.martin.apkanalyzer.core.apps.components.BroadcastReceiver
import sk.styk.martin.apkanalyzer.core.apps.components.ContentProvider
import sk.styk.martin.apkanalyzer.core.apps.components.Service
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.Feature
import sk.styk.martin.apkanalyzer.core.apps.installsource.InstallSourceChain
import sk.styk.martin.apkanalyzer.core.apps.packaging.InstalledSplitApk
import sk.styk.martin.apkanalyzer.core.apps.packaging.SplitApkKind
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permission
import sk.styk.martin.apkanalyzer.core.apps.permissions.PermissionDetails
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permissions
import sk.styk.martin.apkanalyzer.core.apps.permissions.ProtectionLevel
import sk.styk.martin.apkanalyzer.core.apps.permissions.UsedPermission
import sk.styk.martin.apkanalyzer.core.apps.signing.ApkAnalyzerCertificate
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigning
import sk.styk.martin.apkanalyzer.core.apps.signing.GoogleCertificate
import sk.styk.martin.apkanalyzer.core.apps.signing.WhatsAppCertificate
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.megabytes
import java.time.Instant

private val googlePlay = PackageName("com.android.vending")
private val android = PackageName("android")

private val internetPermission = UsedPermission(
    permissionData = Permission(
        name = "android.permission.INTERNET",
        details = PermissionDetails(
            groupName = null,
            protectionLevel = ProtectionLevel.Normal,
            protectionFlags = emptySet(),
            description = "Allows the app to create network sockets.",
            declaringPackage = android,
        ),
    ),
    isGranted = true,
)

private val cameraPermission = UsedPermission(
    permissionData = Permission(
        name = "android.permission.CAMERA",
        details = PermissionDetails(
            groupName = "android.permission-group.CAMERA",
            protectionLevel = ProtectionLevel.Dangerous,
            protectionFlags = emptySet(),
            description = "Allows the app to take pictures and videos.",
            declaringPackage = android,
        ),
    ),
    isGranted = false,
)

val AppDetail.packageName: String
    get() = info.packageName.value

val ApkAnalyzer = AppDetail(
    analysisMode = AppDetail.AnalysisMode.InstalledPackage,
    info = AppInfo(
        packageName = PackageName("sk.styk.martin.apkanalyzer"),
        applicationName = "Apk Analyzer",
        processName = "sk.styk.martin.apkanalyzer",
        versionName = "3.5.0",
        versionCode = 350,
        isSystemApp = false,
        isDebuggable = false,
        allowsBackup = true,
        usesCleartextTraffic = false,
        uid = 10231,
        sharedUserId = null,
        description = "Inspects installed apps and APK files.",
        source = AppSource.GooglePlay,
        apkDirectory = "/data/app/sk.styk.martin.apkanalyzer/base.apk",
        dataDirectory = "/data/user/0/sk.styk.martin.apkanalyzer",
        installLocation = InstallLocation.Auto,
        installSourceChain = InstallSourceChain(
            installingPackage = googlePlay,
            initiatingPackage = googlePlay,
            originatingPackage = null,
        ),
        apkSize = 24.megabytes,
        firstInstallTime = Instant.parse("2025-01-10T09:00:00Z"),
        lastUpdateTime = Instant.parse("2026-10-01T09:00:00Z"),
        minSdkVersion = 28,
        minSdkLabel = "Android 9",
        targetSdkVersion = 36,
        targetSdkLabel = "Android 16",
        totalSize = 31.megabytes,
        lastUsedTime = Instant.parse("2026-11-01T18:30:00Z"),
        installedSplits = emptyList(),
    ),
    signing = AppSigning(
        currentCertificates = listOf(ApkAnalyzerCertificate),
        pastCertificates = emptyList(),
        hasMultipleSigners = false,
    ),
    activities = listOf(
        Activity(
            name = "sk.styk.martin.apkanalyzer.MainActivity",
            packageName = PackageName("sk.styk.martin.apkanalyzer"),
            label = "Apk Analyzer",
            targetActivity = null,
            permission = null,
            parentName = null,
            isExported = true,
            isLauncher = true,
        ),
    ),
    services = emptyList(),
    contentProviders = listOf(
        ContentProvider(
            name = "androidx.core.content.FileProvider",
            authority = "sk.styk.martin.apkanalyzer",
            readPermission = null,
            writePermission = null,
            isExported = false,
            pathPermissions = emptyList(),
        ),
    ),
    receivers = emptyList(),
    permissions = Permissions(defined = emptyList(), used = listOf(internetPermission)),
    features = emptyList(),
)

val WhatsApp = AppDetail(
    analysisMode = AppDetail.AnalysisMode.InstalledPackage,
    info = AppInfo(
        packageName = PackageName("com.whatsapp"),
        applicationName = "WhatsApp",
        processName = "com.whatsapp",
        versionName = "2.26.20.74",
        versionCode = 262074,
        isSystemApp = false,
        isDebuggable = false,
        allowsBackup = false,
        usesCleartextTraffic = false,
        uid = 10187,
        sharedUserId = null,
        description = "Sends messages and makes calls.",
        source = AppSource.GooglePlay,
        apkDirectory = "/data/app/com.whatsapp/base.apk",
        dataDirectory = "/data/user/0/com.whatsapp",
        installLocation = InstallLocation.Auto,
        installSourceChain = InstallSourceChain(
            installingPackage = googlePlay,
            initiatingPackage = googlePlay,
            originatingPackage = null,
        ),
        apkSize = 92.megabytes,
        firstInstallTime = Instant.parse("2023-05-02T12:00:00Z"),
        lastUpdateTime = Instant.parse("2026-10-20T04:15:00Z"),
        minSdkVersion = 24,
        minSdkLabel = "Android 7.0",
        targetSdkVersion = 35,
        targetSdkLabel = "Android 15",
        totalSize = 1200.megabytes,
        lastUsedTime = Instant.parse("2026-11-05T20:45:00Z"),
        installedSplits = listOf(
            InstalledSplitApk(
                fileName = "split_config.arm64_v8a.apk",
                filePath = "/data/app/com.whatsapp/split_config.arm64_v8a.apk",
                size = 38.megabytes,
                kind = SplitApkKind.Abi,
                qualifier = "arm64_v8a",
            ),
        ),
    ),
    signing = AppSigning(
        currentCertificates = listOf(WhatsAppCertificate),
        pastCertificates = emptyList(),
        hasMultipleSigners = false,
    ),
    activities = listOf(
        Activity(
            name = "com.whatsapp.Main",
            packageName = PackageName("com.whatsapp"),
            label = "WhatsApp",
            targetActivity = null,
            permission = null,
            parentName = null,
            isExported = true,
            isLauncher = true,
        ),
    ),
    services = listOf(
        Service(
            name = "com.whatsapp.push.FcmListenerService",
            permission = null,
            isExported = true,
            isStopWithTask = false,
            isSingleUser = false,
            isIsolatedProcess = false,
            isExternalService = false,
        ),
    ),
    contentProviders = listOf(
        ContentProvider(
            name = "com.whatsapp.contact.ContactProvider",
            authority = "com.whatsapp.provider.contact",
            readPermission = "android.permission.READ_CONTACTS",
            writePermission = "android.permission.WRITE_CONTACTS",
            isExported = true,
            pathPermissions = emptyList(),
        ),
    ),
    receivers = listOf(
        BroadcastReceiver(
            name = "com.whatsapp.BootReceiver",
            permission = null,
            isExported = true,
        ),
    ),
    permissions = Permissions(defined = emptyList(), used = listOf(internetPermission, cameraPermission)),
    features = listOf(
        Feature.Hardware(
            name = "android.hardware.camera",
            version = Feature.VERSION_UNSPECIFIED,
            isRequired = false,
        ),
    ),
)

val Gmail = AppDetail(
    analysisMode = AppDetail.AnalysisMode.InstalledPackage,
    info = AppInfo(
        packageName = PackageName("com.google.android.gm"),
        applicationName = "Gmail",
        processName = "com.google.android.gm",
        versionName = "2026.10.12.0",
        versionCode = 65500000,
        isSystemApp = true,
        isDebuggable = false,
        allowsBackup = true,
        usesCleartextTraffic = false,
        uid = 10112,
        sharedUserId = null,
        description = "Sends and receives email.",
        source = AppSource.SystemPreinstalled,
        apkDirectory = "/product/app/Gmail2/Gmail2.apk",
        dataDirectory = "/data/user/0/com.google.android.gm",
        installLocation = InstallLocation.Internal,
        installSourceChain = InstallSourceChain(
            installingPackage = null,
            initiatingPackage = null,
            originatingPackage = null,
        ),
        apkSize = 210.megabytes,
        firstInstallTime = Instant.parse("2009-01-01T00:00:00Z"),
        lastUpdateTime = Instant.parse("2026-10-15T02:00:00Z"),
        minSdkVersion = 26,
        minSdkLabel = "Android 8.0",
        targetSdkVersion = 36,
        targetSdkLabel = "Android 16",
        totalSize = 640.megabytes,
        lastUsedTime = Instant.parse("2026-11-04T07:20:00Z"),
        installedSplits = emptyList(),
    ),
    signing = AppSigning(
        currentCertificates = listOf(GoogleCertificate),
        pastCertificates = emptyList(),
        hasMultipleSigners = false,
    ),
    activities = listOf(
        Activity(
            name = "com.google.android.gm.ConversationListActivityGmail",
            packageName = PackageName("com.google.android.gm"),
            label = "Gmail",
            targetActivity = null,
            permission = null,
            parentName = null,
            isExported = true,
            isLauncher = true,
        ),
    ),
    services = listOf(
        Service(
            name = "com.google.android.gm.job.GmailJobService",
            permission = "android.permission.BIND_JOB_SERVICE",
            isExported = false,
            isStopWithTask = false,
            isSingleUser = false,
            isIsolatedProcess = false,
            isExternalService = false,
        ),
    ),
    contentProviders = emptyList(),
    receivers = listOf(
        BroadcastReceiver(
            name = "com.google.android.gm.GmailBroadcastReceiver",
            permission = null,
            isExported = false,
        ),
    ),
    permissions = Permissions(defined = emptyList(), used = listOf(internetPermission)),
    features = emptyList(),
)
