package sk.styk.martin.apkanalyzer.core.appindex.model

import sk.styk.martin.apkanalyzer.core.apps.model.AppCategory
import sk.styk.martin.apkanalyzer.core.apps.signing.Certificate
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

data class AppAttributeIndex(
    val targetSdk: Map<Int, List<PackageName>> = emptyMap(),
    val minSdk: Map<Int, List<PackageName>> = emptyMap(),
    val installSource: Map<AppSource, List<PackageName>> = emptyMap(),
    val permission: Map<String, List<PackageName>> = emptyMap(),
    val certificateSha256: Map<String, List<PackageName>> = emptyMap(),
    val certificateSha1: Map<String, List<PackageName>> = emptyMap(),
    val certificateMd5: Map<String, List<PackageName>> = emptyMap(),
    val certificateOrganization: Map<String?, List<PackageName>> = emptyMap(),
    val certificateCountry: Map<String?, List<PackageName>> = emptyMap(),
    val certificateByHash: Map<String, Certificate> = emptyMap(),
    val sharedUserId: Map<String, List<PackageName>> = emptyMap(),
    val appCategory: Map<AppCategory, List<PackageName>> = emptyMap(),
)
