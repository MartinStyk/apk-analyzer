package sk.styk.martin.apkanalyzer.core.appexport.writer

internal sealed interface InventoryValue {
    @JvmInline
    value class Text(val value: String) : InventoryValue

    @JvmInline
    value class Number(val value: Long) : InventoryValue

    @JvmInline
    value class Flag(val value: Boolean) : InventoryValue

    @JvmInline
    value class TextList(val values: List<String>) : InventoryValue

    data object Missing : InventoryValue
}
