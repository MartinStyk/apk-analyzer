package sk.styk.martin.apkanalyzer.core.appexport

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class InventoryFormat(val extension: String, val mimeType: String) {
    Csv("csv", "text/csv"),
    Json("json", "application/json"),
    ;

    fun fileName(createdAt: LocalDateTime): String = "apk-analyzer-apps-${createdAt.format(FILE_NAME_TIMESTAMP)}.$extension"
}

private val FILE_NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")
