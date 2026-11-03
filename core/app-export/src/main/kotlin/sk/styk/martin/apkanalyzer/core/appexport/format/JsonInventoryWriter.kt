package sk.styk.martin.apkanalyzer.core.appexport.format

import android.util.JsonWriter
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue
import java.io.Writer
import java.time.Instant

internal class JsonInventoryWriter(
    private val columns: List<InventoryColumn>,
    output: Writer,
    private val generatedAt: Instant,
) : InventoryWriter {

    private val json = JsonWriter(output).apply { setIndent("  ") }

    override fun begin() {
        json.beginObject()
        json.name("\$schema").value(INVENTORY_SCHEMA_URL)
        json.name("schema_version").value(INVENTORY_SCHEMA_VERSION.toLong())
        json.name("generated_at").value(generatedAt.toString())
        json.name("apps").beginArray()
    }

    override fun writeApp(values: List<InventoryValue>) {
        json.beginObject()
        columns.zip(values).forEach { (column, value) ->
            json.name(column.key)
            json.writeValue(value)
        }
        json.endObject()
    }

    override fun close() {
        try {
            json.endArray()
            json.endObject()
        } finally {
            json.close()
        }
    }
}

private fun JsonWriter.writeValue(value: InventoryValue) {
    when (value) {
        is InventoryValue.Text -> value(value.value)

        is InventoryValue.Number -> value(value.value)

        is InventoryValue.Flag -> value(value.value)

        is InventoryValue.TextList -> {
            beginArray()
            value.values.forEach { value(it) }
            endArray()
        }

        is InventoryValue.Record -> {
            beginObject()
            value.fields.forEach { (key, field) ->
                name(key)
                writeValue(field)
            }
            endObject()
        }

        is InventoryValue.Items -> {
            beginArray()
            value.values.forEach { writeValue(it) }
            endArray()
        }

        InventoryValue.Missing -> nullValue()
    }
}
