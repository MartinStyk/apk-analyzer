package sk.styk.martin.apkanalyzer.core.appexport.writer

import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttribute
import java.io.Writer

private val FORMULA_PREFIXES = setOf('=', '+', '-', '@', '\t', '\r', '\n')
private val CHARACTERS_REQUIRING_QUOTES = setOf(',', '"', '\r', '\n')

internal class CsvInventoryWriter(private val attributes: List<InventoryAttribute>, private val output: Writer) : InventoryWriter {

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
