package sk.styk.martin.apkanalyzer.core.appexport.column

internal sealed interface InventoryValue {
    @JvmInline
    value class Text(val value: String) : InventoryValue

    @JvmInline
    value class Number(val value: Long) : InventoryValue

    @JvmInline
    value class Flag(val value: Boolean) : InventoryValue

    @JvmInline
    value class TextList(val values: List<String>) : InventoryValue

    @JvmInline
    value class Record(val fields: Map<String, InventoryValue>) : InventoryValue

    @JvmInline
    value class Items(val values: List<InventoryValue>) : InventoryValue

    data object Missing : InventoryValue
}
