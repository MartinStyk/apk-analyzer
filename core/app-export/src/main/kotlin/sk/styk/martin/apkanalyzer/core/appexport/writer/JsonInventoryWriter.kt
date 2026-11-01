package sk.styk.martin.apkanalyzer.core.appexport.writer

import android.util.JsonWriter
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttribute
import java.io.Writer

internal class JsonInventoryWriter(private val attributes: List<InventoryAttribute>, output: Writer) : InventoryWriter {

    private val json = JsonWriter(output).apply { setIndent("  ") }

    override fun begin() {
        json.beginArray()
    }

    override fun writeApp(values: List<InventoryValue>) {
        json.beginObject()
        attributes.zip(values).forEach { (attribute, value) ->
            json.name(attribute.key)
            json.writeValue(value)
        }
        json.endObject()
    }

    override fun end() {
        json.endArray()
        json.flush()
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

        InventoryValue.Missing -> nullValue()
    }
}
