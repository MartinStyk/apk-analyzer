package sk.styk.martin.apkanalyzer.core.apps.analysis

import android.content.pm.PackageInfo
import sk.styk.martin.apkanalyzer.core.apps.model.CertificateData

interface CertificateExtractor {
    fun getCertificateData(packageInfo: PackageInfo): List<CertificateData>
}
