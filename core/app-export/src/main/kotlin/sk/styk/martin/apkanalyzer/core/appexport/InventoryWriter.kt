package sk.styk.martin.apkanalyzer.core.appexport

import android.util.JsonWriter
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

private class CsvInventoryWriter(private val attributes: List<InventoryAttribute>, private val output: Writer) : InventoryWriter {

    override fun begin() {
        writeRow(attributes.map { it.key })
    }

    override fun writeApp(values: List<InventoryValue>) {
        writeRow(values.map { it.toCsvCell() })
    }

    override fun end() {
        output.flush()
    }

    private fun writeRow(cells: List<String>) {
        output.write(cells.joinToString(separator = ",") { it.quoted() })
        output.write("\r\n")
    }

    private fun InventoryValue.toCsvCell(): String = when (this) {
        is InventoryValue.Text -> value.guardedAgainstFormula()
        is InventoryValue.Number -> value.toString()
        is InventoryValue.Flag -> value.toString()
        is InventoryValue.TextList -> values.joinToString(separator = "; ").guardedAgainstFormula()
        InventoryValue.Missing -> ""
    }

    private fun String.guardedAgainstFormula(): String = if (firstOrNull() in FORMULA_PREFIXES) "'$this" else this

    private fun String.quoted(): String = if (any { it in CHARACTERS_REQUIRING_QUOTES }) "\"${replace("\"", "\"\"")}\"" else this

    private companion object {
        val FORMULA_PREFIXES = setOf('=', '+', '-', '@', '\t', '\r', '\n')
        val CHARACTERS_REQUIRING_QUOTES = setOf(',', '"', '\r', '\n')
    }
}

private class JsonInventoryWriter(private val attributes: List<InventoryAttribute>, output: Writer) : InventoryWriter {

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
}
