package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.PackageInfo
import sk.styk.martin.apkanalyzer.core.apps.model.AppSigning
import sk.styk.martin.apkanalyzer.core.apps.model.SigningSchemeVersion

interface CertificateExtractor {
    fun getAppSigning(packageInfo: PackageInfo): AppSigning
    fun resolveSigningSchemeVersions(packageInfo: PackageInfo): List<SigningSchemeVersion>?
}
