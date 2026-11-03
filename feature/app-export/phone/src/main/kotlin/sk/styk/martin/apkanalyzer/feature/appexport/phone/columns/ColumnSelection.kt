package sk.styk.martin.apkanalyzer.feature.appexport.phone.columns

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumnGroup
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryPreset

internal class ColumnSelection {

    private val selected = MutableStateFlow(InventoryPreset.Default.columns)
    val columns: StateFlow<Set<InventoryColumn>> = selected.asStateFlow()

    fun replace(columns: Set<InventoryColumn>) {
        selected.value = columns
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
        selected.update { if (it.containsAll(targets)) it - targets.toSet() else it + targets }
    }
}
