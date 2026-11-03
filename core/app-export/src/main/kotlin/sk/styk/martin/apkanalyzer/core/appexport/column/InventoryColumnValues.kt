package sk.styk.martin.apkanalyzer.core.appexport.column

import sk.styk.martin.apkanalyzer.core.appexport.source.InventoryAppData
import sk.styk.martin.apkanalyzer.core.appexport.source.valueOrNull
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.time.Instant

internal fun InventoryColumn.valueOf(data: InventoryAppData): InventoryValue = extract(data).also {
    check(shape == InventoryColumnShape.Structured || !it.isStructured) {
        "Column $key is declared scalar but produced a structured value"
    }
}

private val InventoryValue.isStructured: Boolean
    get() = this is InventoryValue.Record || this is InventoryValue.Items

@Suppress("CyclomaticComplexMethod")
private fun InventoryColumn.extract(data: InventoryAppData): InventoryValue {
    val app = data.app
    val info = data.detail.valueOrNull()?.info
    return when (this) {
        InventoryColumn.ApplicationName -> text(app.applicationName)

        InventoryColumn.PackageName -> text(app.packageName.value)

        InventoryColumn.ProcessName -> text(info?.processName)

        InventoryColumn.Uid -> number(info?.uid?.toLong())

        InventoryColumn.SharedUserId -> text(app.sharedUserId)

        InventoryColumn.Category -> text(app.category.token)

        InventoryColumn.VersionName -> text(app.versionName)

        InventoryColumn.VersionCode -> InventoryValue.Number(app.version)

        InventoryColumn.MinSdk -> InventoryValue.Number(app.minSdk.toLong())

        InventoryColumn.TargetSdk -> InventoryValue.Number(app.targetSdk.toLong())

        InventoryColumn.AppType -> InventoryValue.Text(if (app.isSystemApp) "system" else "user")

        InventoryColumn.InstallSource -> InventoryValue.Text(app.source.token)

        InventoryColumn.InstallerPackage -> text(app.installSourceChain.installingPackage)

        InventoryColumn.InstallInitiator -> text(app.installSourceChain.initiatingPackage)

        InventoryColumn.InstallOriginator -> text(app.installSourceChain.originatingPackage)

        InventoryColumn.FirstInstalled -> timestamp(app.installTime)

        InventoryColumn.LastUpdated -> timestamp(app.lastUpdateTime)

        InventoryColumn.LastUsed -> timestamp(app.lastUsedTime)

        InventoryColumn.ApkSize -> InventoryValue.Number(app.apkSize.bytes)

        InventoryColumn.TotalSize -> number(app.totalSize?.bytes)

        InventoryColumn.SplitApks -> list(info?.installedSplits?.map { it.fileName })

        InventoryColumn.InstallLocation -> text(info?.installLocation?.token)

        InventoryColumn.ApkDirectory -> text(info?.apkDirectory)

        InventoryColumn.DataDirectory -> text(info?.dataDirectory)

        InventoryColumn.Debuggable -> flag(info?.isDebuggable)

        InventoryColumn.AllowsBackup -> flag(info?.allowsBackup)

        InventoryColumn.CleartextTraffic -> flag(info?.usesCleartextTraffic)

        InventoryColumn.SigningCertificateSha256 -> list(data.signing.valueOrNull()?.currentCertificates?.map { it.certificateHashSha256 })

        InventoryColumn.SigningCertificateSubject -> list(data.signing.valueOrNull()?.currentCertificates?.mapNotNull { it.subject.displayName })

        InventoryColumn.SigningCertificateMd5 -> list(data.signing.valueOrNull()?.currentCertificates?.map { it.certificateHashMd5 })

        InventoryColumn.SigningCertificateSha1 -> list(data.signing.valueOrNull()?.currentCertificates?.map { it.certificateHashSha1 })

        InventoryColumn.SigningPublicKeyMd5 -> list(data.signing.valueOrNull()?.currentCertificates?.map { it.publicKeyMd5 })

        InventoryColumn.SigningPublicKeySha1 -> list(data.signing.valueOrNull()?.currentCertificates?.map { it.publicKeySha1 })

        InventoryColumn.SigningPublicKeySha256 -> list(data.signing.valueOrNull()?.currentCertificates?.map { it.publicKeySha256 })

        InventoryColumn.RequestedPermissions -> InventoryValue.TextList(app.requestedPermissions)

        InventoryColumn.GrantedPermissions -> list(data.detail.valueOrNull()?.permissions?.used?.filter { it.isGranted }?.map { it.permissionData.name })

        InventoryColumn.ActivityCount -> number(data.detail.valueOrNull()?.activities?.size?.toLong())

        InventoryColumn.ServiceCount -> number(data.detail.valueOrNull()?.services?.size?.toLong())

        InventoryColumn.ReceiverCount -> number(data.detail.valueOrNull()?.receivers?.size?.toLong())

        InventoryColumn.ProviderCount -> number(data.detail.valueOrNull()?.contentProviders?.size?.toLong())

        InventoryColumn.Description -> text(info?.description)

        InventoryColumn.SplitApkDetails -> records(info?.installedSplits) { toInventoryValue() }

        InventoryColumn.NativeLibraryAbis -> list(data.nativeLibraries.valueOrNull()?.abis)

        InventoryColumn.NativeLibraryFiles -> records(data.nativeLibraries.valueOrNull()?.files) { toInventoryValue() }

        InventoryColumn.SigningCertificates -> records(data.signing.valueOrNull()?.currentCertificates) { toInventoryValue() }

        InventoryColumn.SigningCertificateHistory -> records(data.signing.valueOrNull()?.pastCertificates) { toInventoryValue() }

        InventoryColumn.HasMultipleSigners -> flag(data.signing.valueOrNull()?.hasMultipleSigners)

        InventoryColumn.SigningSchemes -> list(data.signingSchemes.valueOrNull()?.map { it.token })

        InventoryColumn.DefinedPermissions -> records(data.detail.valueOrNull()?.permissions?.defined) { toInventoryValue() }

        InventoryColumn.PermissionDetails -> records(data.detail.valueOrNull()?.permissions?.used) {
            record("permission" to permissionData.toInventoryValue(), "granted" to flag(isGranted))
        }

        InventoryColumn.Activities -> records(data.detail.valueOrNull()?.activities) { toInventoryValue() }

        InventoryColumn.Services -> records(data.detail.valueOrNull()?.services) { toInventoryValue() }

        InventoryColumn.Receivers -> records(data.detail.valueOrNull()?.receivers) { toInventoryValue() }

        InventoryColumn.Providers -> records(data.detail.valueOrNull()?.contentProviders) { toInventoryValue() }

        InventoryColumn.IntentFilters -> records(data.intentFilters.valueOrNull()?.entries?.sortedWith(compareBy({ it.key.kind.ordinal }, { it.key.name }))) {
            record(
                "component_name" to text(key.name),
                "component_kind" to text(key.kind.token),
                "filters" to records(value) { toInventoryValue() },
            )
        }

        InventoryColumn.Features -> records(data.detail.valueOrNull()?.features) { toInventoryValue() }

        InventoryColumn.Manifest -> data.manifest.valueOrNull()?.let {
            record("xml" to text(it.xml), "additional_installed_splits" to number(it.additionalInstalledSplits.toLong()))
        } ?: InventoryValue.Missing
    }
}

internal fun text(value: String?): InventoryValue = value?.let { InventoryValue.Text(it) } ?: InventoryValue.Missing

private fun text(value: PackageName?): InventoryValue = text(value?.value)

internal fun number(value: Long?): InventoryValue = value?.let { InventoryValue.Number(it) } ?: InventoryValue.Missing

internal fun flag(value: Boolean?): InventoryValue = value?.let { InventoryValue.Flag(it) } ?: InventoryValue.Missing

private fun list(values: List<String>?): InventoryValue = values?.let { InventoryValue.TextList(it) } ?: InventoryValue.Missing

private fun timestamp(value: Instant?): InventoryValue = text(value?.toString())

internal fun record(vararg fields: Pair<String, InventoryValue>): InventoryValue = InventoryValue.Record(fields.toMap())

internal fun <T> records(values: Collection<T>?, transform: T.() -> InventoryValue): InventoryValue =
    values?.let { InventoryValue.Items(it.map(transform)) } ?: InventoryValue.Missing
