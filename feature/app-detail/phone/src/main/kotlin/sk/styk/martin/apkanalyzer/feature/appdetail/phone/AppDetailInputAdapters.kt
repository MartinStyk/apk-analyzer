package sk.styk.martin.apkanalyzer.feature.appdetail.phone

import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput
import java.time.Instant

internal fun AppDetailInput.toLiveReference(): AppReference? = when (this) {
    is AppDetailInput.InstalledPackage -> AppReference.InstalledPackage(PackageName(packageName))
    is AppDetailInput.ApkFile -> AppReference.ApkFile(apkFilePath)
    is AppDetailInput.Historical -> null
}

internal fun AppDetailInput.toIconReference(): AppReference = when (this) {
    is AppDetailInput.InstalledPackage -> AppReference.InstalledPackage(PackageName(packageName))
    is AppDetailInput.ApkFile -> AppReference.ApkFile(apkFilePath)
    is AppDetailInput.Historical -> AppReference.InstalledPackage(PackageName(packageName))
}

internal val AppDetailInput.recordedAt: Instant?
    get() = (this as? AppDetailInput.Historical)?.let { Instant.ofEpochMilli(it.capturedAtEpochMillis) }
