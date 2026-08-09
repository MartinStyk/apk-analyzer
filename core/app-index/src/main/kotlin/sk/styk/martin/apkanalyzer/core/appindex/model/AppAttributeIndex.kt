package sk.styk.martin.apkanalyzer.core.appindex.model

import sk.styk.martin.apkanalyzer.core.apps.model.AppCategory
import sk.styk.martin.apkanalyzer.core.common.model.AppSource
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

data class AppAttributeIndex(
    val targetSdk: Map<Int, List<PackageName>>,
    val minSdk: Map<Int, List<PackageName>>,
    val installSource: Map<AppSource, List<PackageName>>,
    val permission: Map<String, List<PackageName>>,
    val certificateFingerprint: Map<String, List<PackageName>>,
    val certificateOrganization: Map<String?, List<PackageName>>,
    val certificateCountry: Map<String?, List<PackageName>>,
    val sharedUserId: Map<String, List<PackageName>>,
    val appCategory: Map<AppCategory, List<PackageName>>,
)
