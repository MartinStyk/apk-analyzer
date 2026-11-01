package sk.styk.martin.apkanalyzer.feature.apps.api

import kotlinx.serialization.Serializable

@Serializable
enum class AppFilterScope {
    AppList,
    InventoryExport,
}
