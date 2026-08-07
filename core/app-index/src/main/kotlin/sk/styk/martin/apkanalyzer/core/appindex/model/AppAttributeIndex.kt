package sk.styk.martin.apkanalyzer.core.appindex.model

import sk.styk.martin.apkanalyzer.core.common.model.AppSource

data class AppAttributeIndex(
    val targetSdk: Map<Int, List<String>>,
    val minSdk: Map<Int, List<String>>,
    val installSource: Map<AppSource, List<String>>,
    val permission: Map<String, List<String>>,
    val certificateFingerprint: Map<String, List<String>>,
    val certificateOrganization: Map<String?, List<String>>,
    val certificateCountry: Map<String?, List<String>>,
)
