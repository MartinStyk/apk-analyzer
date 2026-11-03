package sk.styk.martin.apkanalyzer.core.appexport.format

import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryValue
import java.io.Writer

private val FORMULA_PREFIXES = setOf('=', '+', '-', '@', '\t', '\r', '\n')
private val CHARACTERS_REQUIRING_QUOTES = setOf(',', '"', '\r', '\n')

internal class CsvInventoryWriter(private val columns: List<InventoryColumn>, private val output: Writer) : InventoryWriter {

    init {
        require(InventoryFormat.Csv.supports(columns)) { "CSV does not support detailed records; use JSON" }
    }

    override fun begin() {
        writeRow(columns.map { it.key })
    }

    override fun writeApp(values: List<InventoryValue>) {
        writeRow(values.map { it.toCsvCell() })
    }

    override fun close() {
        output.close()
    }

    private fun writeRow(cells: List<String>) {
        output.write(cells.joinToString(separator = ",") { it.quoted() })
        output.write("\r\n")
    }
}

private fun InventoryValue.toCsvCell(): String = when (this) {
    is InventoryValue.Text -> value.guardedAgainstFormula()
    is InventoryValue.Number -> value.toString()
    is InventoryValue.Flag -> value.toString()
    is InventoryValue.TextList -> values.joinToString(separator = "; ").guardedAgainstFormula()
    is InventoryValue.Record, is InventoryValue.Items -> error("CSV does not support detailed records; use JSON")
    InventoryValue.Missing -> ""
}

private fun String.guardedAgainstFormula(): String = if (firstOrNull() in FORMULA_PREFIXES) "'$this" else this

private fun String.quoted(): String = if (any { it in CHARACTERS_REQUIRING_QUOTES }) "\"${replace("\"", "\"\"")}\"" else this
