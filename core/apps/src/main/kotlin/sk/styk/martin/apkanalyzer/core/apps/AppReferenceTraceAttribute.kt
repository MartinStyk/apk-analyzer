package sk.styk.martin.apkanalyzer.core.apps

import sk.styk.martin.apkanalyzer.core.common.model.AppReference

internal val AppReference.analysisModeAttribute: String
    get() = when (this) {
        is AppReference.InstalledPackage -> "installed"
        is AppReference.ApkFile -> "apk_file"
    }
