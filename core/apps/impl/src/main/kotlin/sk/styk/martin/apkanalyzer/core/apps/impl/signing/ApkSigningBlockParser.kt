package sk.styk.martin.apkanalyzer.core.apps.signing

internal interface ApkSigningBlockParser {
    fun parseSchemeVersions(apkPath: String): Set<SigningSchemeVersion>?
}
