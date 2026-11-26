package sk.styk.martin.apkanalyzer.core.apps.signing

import android.content.pm.PackageInfo

internal interface CertificateExtractor {
    fun getAppSigning(packageInfo: PackageInfo): AppSigning
}
