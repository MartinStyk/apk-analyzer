package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailInput

internal fun AppDetailInput.toAppReference(): AppReference = when (this) {
    is AppDetailInput.InstalledPackage -> AppReference.InstalledPackage(PackageName(packageName))
    is AppDetailInput.ApkFile -> AppReference.ApkFile(apkFilePath)
}
