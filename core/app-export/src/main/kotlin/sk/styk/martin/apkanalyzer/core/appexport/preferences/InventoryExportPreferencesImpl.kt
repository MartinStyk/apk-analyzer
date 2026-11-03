package sk.styk.martin.apkanalyzer.core.appexport.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import sk.styk.martin.apkanalyzer.core.appexport.column.InventoryColumn
import sk.styk.martin.apkanalyzer.core.appexport.format.InventoryFormat
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepository
import javax.inject.Inject

internal class InventoryExportPreferencesImpl @Inject constructor(private val persistenceRepository: PersistenceRepository) : InventoryExportPreferences {

    override fun observeLastSelection(): Flow<InventorySelection?> = combine(
        persistenceRepository.observe(Key.InventoryExportColumns),
        persistenceRepository.observe(Key.InventoryExportFormat),
    ) { savedKeys, savedFormat ->
        val columns = InventoryColumn.entries.filter { it.key in savedKeys }.toSet()
        val format = InventoryFormat.entries.firstOrNull { it.name == savedFormat }
        if (columns.isEmpty() || format == null) null else InventorySelection(columns, format)
    }

    override suspend fun saveSelection(selection: InventorySelection) {
        persistenceRepository.save(Key.InventoryExportColumns, selection.columns.map { it.key }.toSet())
        persistenceRepository.save(Key.InventoryExportFormat, selection.format.name)
    }

    override fun observeIntroDismissed(): Flow<Boolean> = persistenceRepository.observe(Key.InventoryExportIntroDismissed)

    override suspend fun dismissIntro() {
        persistenceRepository.save(Key.InventoryExportIntroDismissed, true)
    }
}
