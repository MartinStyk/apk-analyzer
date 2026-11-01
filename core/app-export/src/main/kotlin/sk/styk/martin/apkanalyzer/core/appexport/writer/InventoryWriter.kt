package sk.styk.martin.apkanalyzer.core.appexport.writer

import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttribute
import sk.styk.martin.apkanalyzer.core.appexport.InventoryFormat
import java.io.Writer

internal interface InventoryWriter {
    fun begin()
    fun writeApp(values: List<InventoryValue>)
    fun end()
}

internal fun InventoryFormat.createWriter(attributes: List<InventoryAttribute>, output: Writer): InventoryWriter = when (this) {
    InventoryFormat.Csv -> CsvInventoryWriter(attributes, output)
    InventoryFormat.Json -> JsonInventoryWriter(attributes, output)
}
