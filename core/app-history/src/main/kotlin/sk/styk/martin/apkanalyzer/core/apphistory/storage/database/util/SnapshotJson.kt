package sk.styk.martin.apkanalyzer.core.apphistory.storage.database.util

import kotlinx.serialization.json.Json

internal const val BLOB_FORMAT_VERSION = 1

internal val snapshotJson = Json {
    explicitNulls = false
    ignoreUnknownKeys = true
}
