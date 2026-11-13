package sk.styk.martin.apkanalyzer.feature.appexport.phone.columns

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumnGroup
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryPreset

internal class ColumnSelection {

    val columns: StateFlow<Set<InventoryColumn>>
        field = MutableStateFlow(InventoryPreset.Default.columns)

    fun replace(selection: Set<InventoryColumn>) {
        columns.value = selection
    }

    fun toggle(column: InventoryColumn) {
        toggle(listOf(column))
    }

    fun toggleGroup(group: InventoryColumnGroup) {
        toggle(InventoryColumn.entries.filter { it.group == group })
    }

    fun toggleAll() {
        toggle(InventoryColumn.entries)
    }

    private fun toggle(targets: Collection<InventoryColumn>) {
        columns.update { if (it.containsAll(targets)) it - targets.toSet() else it + targets }
    }
}
