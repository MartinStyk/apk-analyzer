package sk.styk.martin.apkanalyzer.core.appexport.preferences

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.format.InventoryFormat

data class InventorySelection(val columns: Set<InventoryColumn>, val format: InventoryFormat)

interface InventoryExportPreferences {
    fun observeLastSelection(): Flow<InventorySelection?>

    suspend fun saveSelection(selection: InventorySelection)

    fun observeIntroDismissed(): Flow<Boolean>

    suspend fun dismissIntro()
}
