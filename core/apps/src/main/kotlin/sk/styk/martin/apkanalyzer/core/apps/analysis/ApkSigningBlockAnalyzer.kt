package sk.styk.martin.apkanalyzer.core.apps.analysis

import sk.styk.martin.apkanalyzer.core.apps.model.SigningSchemeVersion

interface ApkSigningBlockAnalyzer {
    fun detectSchemeVersions(apkPath: String): List<SigningSchemeVersion>?
}
