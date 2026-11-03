package sk.styk.martin.apkanalyzer.core.appexport.format

import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumnShape
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class InventoryFormat(val extension: String, val mimeType: String) {
    Csv("csv", "text/csv"),
    Json("json", "application/json"),
    ;

    fun fileName(createdAt: LocalDateTime): String = "apk-analyzer-apps-${createdAt.format(FILE_NAME_TIMESTAMP)}.$extension"

    fun supports(column: InventoryColumn): Boolean = when (this) {
        Csv -> column.shape == InventoryColumnShape.Scalar
        Json -> true
    }

    fun supports(columns: Collection<InventoryColumn>): Boolean = columns.all(::supports)
}

const val INVENTORY_REFERENCE_URL = "https://martinstyk.github.io/apk-analyzer/inventory-export/"
internal const val INVENTORY_SCHEMA_VERSION = 1
internal const val INVENTORY_SCHEMA_URL = "${INVENTORY_REFERENCE_URL}v$INVENTORY_SCHEMA_VERSION/schema.json"

private val FILE_NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")
