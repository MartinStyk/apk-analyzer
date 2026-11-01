package sk.styk.martin.apkanalyzer.core.appexport

import sk.styk.martin.apkanalyzer.core.apps.model.AppCategory
import sk.styk.martin.apkanalyzer.core.apps.model.AppDetail
import sk.styk.martin.apkanalyzer.core.apps.model.InstallLocation
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigning
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.time.Instant

internal sealed interface InventoryValue {
    data class Text(val value: String) : InventoryValue
    data class Number(val value: Long) : InventoryValue
    data class Flag(val value: Boolean) : InventoryValue
    data class TextList(val values: List<String>) : InventoryValue
    data object Missing : InventoryValue
}

internal class InventoryAppData(
    val app: InstalledApp,
    val signing: AppSigning?,
    val detail: AppDetail?,
)

@Suppress("CyclomaticComplexMethod")
internal fun InventoryAttribute.valueOf(data: InventoryAppData): InventoryValue {
    val app = data.app
    val info = data.detail?.info
    return when (this) {
        InventoryAttribute.ApplicationName -> text(app.applicationName)
        InventoryAttribute.PackageName -> text(app.packageName.value)
        InventoryAttribute.ProcessName -> text(info?.processName)
        InventoryAttribute.Uid -> number(info?.uid?.toLong())
        InventoryAttribute.SharedUserId -> text(app.sharedUserId)
        InventoryAttribute.Category -> text(app.category.token)
        InventoryAttribute.VersionName -> text(app.versionName)
        InventoryAttribute.VersionCode -> InventoryValue.Number(app.version)
        InventoryAttribute.MinSdk -> InventoryValue.Number(app.minSdk.toLong())
        InventoryAttribute.TargetSdk -> InventoryValue.Number(app.targetSdk.toLong())
        InventoryAttribute.AppType -> InventoryValue.Text(if (app.isSystemApp) "system" else "user")
        InventoryAttribute.InstallSource -> InventoryValue.Text(app.source.token)
        InventoryAttribute.InstallerPackage -> text(app.installSourceChain.installingPackage)
        InventoryAttribute.InstallInitiator -> text(app.installSourceChain.initiatingPackage)
        InventoryAttribute.InstallOriginator -> text(app.installSourceChain.originatingPackage)
        InventoryAttribute.FirstInstalled -> timestamp(app.installTime)
        InventoryAttribute.LastUpdated -> timestamp(app.lastUpdateTime)
        InventoryAttribute.LastUsed -> timestamp(app.lastUsedTime)
        InventoryAttribute.ApkSize -> InventoryValue.Number(app.apkSize.bytes)
        InventoryAttribute.TotalSize -> number(app.totalSize?.bytes)
        InventoryAttribute.SplitApks -> list(info?.installedSplits?.map { it.fileName })
        InventoryAttribute.InstallLocation -> text(info?.installLocation?.token)
        InventoryAttribute.ApkDirectory -> text(info?.apkDirectory)
        InventoryAttribute.DataDirectory -> text(info?.dataDirectory)
        InventoryAttribute.Debuggable -> flag(info?.isDebuggable)
        InventoryAttribute.AllowsBackup -> flag(info?.allowsBackup)
        InventoryAttribute.CleartextTraffic -> flag(info?.usesCleartextTraffic)
        InventoryAttribute.SigningCertificateSha256 -> list(data.signing?.currentCertificates?.map { it.formattedSha256Fingerprint })
        InventoryAttribute.SigningCertificateSubject -> list(data.signing?.currentCertificates?.mapNotNull { it.subject.displayName })
        InventoryAttribute.RequestedPermissions -> InventoryValue.TextList(app.requestedPermissions)
        InventoryAttribute.GrantedPermissions -> list(data.detail?.permissions?.used?.filter { it.isGranted }?.map { it.permissionData.name })
        InventoryAttribute.ActivityCount -> number(data.detail?.activities?.size?.toLong())
        InventoryAttribute.ServiceCount -> number(data.detail?.services?.size?.toLong())
        InventoryAttribute.ReceiverCount -> number(data.detail?.receivers?.size?.toLong())
        InventoryAttribute.ProviderCount -> number(data.detail?.contentProviders?.size?.toLong())
    }
}

private fun text(value: String?): InventoryValue = value?.takeIf { it.isNotBlank() }?.let { InventoryValue.Text(it) } ?: InventoryValue.Missing

private fun text(value: PackageName?): InventoryValue = text(value?.value)

private fun number(value: Long?): InventoryValue = value?.let { InventoryValue.Number(it) } ?: InventoryValue.Missing

private fun flag(value: Boolean?): InventoryValue = value?.let { InventoryValue.Flag(it) } ?: InventoryValue.Missing

private fun list(values: List<String>?): InventoryValue = values?.let { InventoryValue.TextList(it) } ?: InventoryValue.Missing

private fun timestamp(value: Instant?): InventoryValue = text(value?.toString())

private val AppCategory.token: String?
    get() = when (this) {
        AppCategory.Undefined -> null
        AppCategory.Game -> "game"
        AppCategory.Audio -> "audio"
        AppCategory.Video -> "video"
        AppCategory.Image -> "image"
        AppCategory.Social -> "social"
        AppCategory.News -> "news"
        AppCategory.Maps -> "maps"
        AppCategory.Productivity -> "productivity"
        AppCategory.Accessibility -> "accessibility"
    }

private val AppSource.token: String
    get() = when (this) {
        AppSource.GooglePlay -> "google_play"
        AppSource.SamsungGalaxyStore -> "samsung_galaxy_store"
        AppSource.AmazonAppstore -> "amazon_appstore"
        AppSource.HuaweiAppGallery -> "huawei_app_gallery"
        AppSource.XiaomiGetApps -> "xiaomi_get_apps"
        AppSource.FDroid -> "f_droid"
        AppSource.AuroraStore -> "aurora_store"
        AppSource.Sideloaded -> "sideloaded"
        AppSource.LocalInstall -> "local_install"
        AppSource.SystemPreinstalled -> "system_preinstalled"
        AppSource.Unknown -> "unknown"
    }

private val InstallLocation.token: String
    get() = when (this) {
        InstallLocation.Auto -> "auto"
        InstallLocation.Internal -> "internal_only"
        InstallLocation.External -> "prefer_external"
    }
