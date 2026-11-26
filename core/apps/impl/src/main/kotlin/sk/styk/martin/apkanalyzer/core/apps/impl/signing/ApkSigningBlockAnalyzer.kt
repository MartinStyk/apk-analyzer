package sk.styk.martin.apkanalyzer.core.apps.impl.signing

import sk.styk.martin.apkanalyzer.core.apps.api.signing.SigningSchemeVersion

internal interface ApkSigningBlockAnalyzer {
    fun detectSchemeVersions(apkPath: String): List<SigningSchemeVersion>?
}
