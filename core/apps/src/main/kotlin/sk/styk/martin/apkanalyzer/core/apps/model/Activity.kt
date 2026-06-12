package sk.styk.martin.apkanalyzer.core.apps.model

data class Activity(
    val name: String,
    val packageName: String,
    val label: String? = null,
    val targetActivity: String? = null,
    val permission: String? = null,
    val parentName: String? = null,
    val isExported: Boolean = false,
)
