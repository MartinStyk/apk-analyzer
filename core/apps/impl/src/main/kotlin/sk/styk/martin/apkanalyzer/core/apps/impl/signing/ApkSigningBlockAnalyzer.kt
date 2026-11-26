package sk.styk.martin.apkanalyzer.core.apps.signing

internal interface ApkSigningBlockAnalyzer {
    fun detectSchemeVersions(apkPath: String): List<SigningSchemeVersion>?
}
