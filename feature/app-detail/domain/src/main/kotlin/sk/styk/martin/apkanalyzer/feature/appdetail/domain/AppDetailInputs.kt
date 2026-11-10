package sk.styk.martin.apkanalyzer.feature.appdetail.domain

import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import java.time.Instant

val AppDetailInput.isHistorical: Boolean
    get() = this is AppDetailInput.Historical

val AppDetailInput.isOnDevice: Boolean
    get() = !isHistorical

val AppDetailInput.liveReference: AppReference?
    get() = when (this) {
        is AppDetailInput.InstalledPackage -> AppReference.InstalledPackage(PackageName(packageName))
        is AppDetailInput.ApkFile -> AppReference.ApkFile(apkFilePath)
        is AppDetailInput.Historical -> null
    }

val AppDetailInput.iconReference: AppReference
    get() = when (this) {
        is AppDetailInput.Historical -> AppReference.InstalledPackage(PackageName(packageName))
        else -> checkNotNull(liveReference)
    }

val AppDetailInput.installedPackage: PackageName?
    get() = (this as? AppDetailInput.InstalledPackage)?.let { PackageName(it.packageName) }

val AppDetailInput.recordedAt: Instant?
    get() = (this as? AppDetailInput.Historical)?.capturedAt
