package sk.styk.martin.apkanalyzer.core.appexport.impl.format

import sk.styk.martin.apkanalyzer.core.appexport.api.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.api.format.InventoryFormat
import sk.styk.martin.apkanalyzer.core.appexport.impl.column.InventoryValue
import java.io.Writer
import java.time.Instant

internal interface InventoryWriter : AutoCloseable {
    fun begin()
    fun writeApp(values: List<InventoryValue>)
}

internal fun InventoryFormat.createWriter(
    columns: List<InventoryColumn>,
    output: Writer,
    generatedAt: Instant,
): InventoryWriter = when (this) {
    InventoryFormat.Csv -> CsvInventoryWriter(columns, output)
    InventoryFormat.Json -> JsonInventoryWriter(columns, output, generatedAt)
}
