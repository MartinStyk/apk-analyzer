package sk.styk.martin.apkanalyzer.core.apps.impl.signing

import android.content.pm.PackageInfo
import sk.styk.martin.apkanalyzer.core.apps.api.signing.AppSigning

internal interface CertificateExtractor {
    fun getAppSigning(packageInfo: PackageInfo): AppSigning
}
