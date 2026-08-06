package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.PackageInfo
import sk.styk.martin.apkanalyzer.core.apps.model.AppSigning

interface CertificateExtractor {
    fun getAppSigning(packageInfo: PackageInfo): AppSigning
}
