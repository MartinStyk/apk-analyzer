package sk.styk.martin.apkanalyzer.core.apps.analysis

import sk.styk.martin.apkanalyzer.core.apps.model.SigningSchemeVersion

internal interface ApkSigningBlockParser {
    fun parseSchemeVersions(apkPath: String): Set<SigningSchemeVersion>?
}
