package sk.styk.martin.apkanalyzer.core.apps.impl.signing

import sk.styk.martin.apkanalyzer.core.apps.api.signing.SigningSchemeVersion

internal interface ApkSigningBlockParser {
    fun parseSchemeVersions(apkPath: String): Set<SigningSchemeVersion>?
}
