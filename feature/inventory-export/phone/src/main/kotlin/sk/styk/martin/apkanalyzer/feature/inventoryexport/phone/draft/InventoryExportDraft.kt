package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.draft

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttribute
import sk.styk.martin.apkanalyzer.core.appexport.InventoryAttributeGroup
import sk.styk.martin.apkanalyzer.core.appexport.InventoryFormat
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.filtering.AppFilterRepository
import sk.styk.martin.apkanalyzer.core.apps.filtering.AppFilterState
import sk.styk.martin.apkanalyzer.core.apps.filtering.InventoryExportFilter
import sk.styk.martin.apkanalyzer.core.apps.filtering.filterBy
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.sorting.AppSortType
import sk.styk.martin.apkanalyzer.core.apps.sorting.comparator
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryExportDraft @Inject constructor(
    installedAppsRepository: InstalledAppsRepository,
    private val listFilterRepository: AppFilterRepository,
    @InventoryExportFilter private val exportFilterRepository: AppFilterRepository,
    dispatcherProvider: DispatcherProvider,
) {

    private val mutableAttributes = MutableStateFlow(InventoryAttribute.Defaults)
    val attributes: StateFlow<Set<InventoryAttribute>> = mutableAttributes.asStateFlow()

    private val mutableFormat = MutableStateFlow(InventoryFormat.Csv)
    val format: StateFlow<InventoryFormat> = mutableFormat.asStateFlow()

    val filter: StateFlow<AppFilterState> = exportFilterRepository.filter

    val apps: Flow<List<InstalledApp>> = combine(
        installedAppsRepository.apps(),
        exportFilterRepository.filter,
    ) { apps, filter ->
        apps.filterBy(filter).sortedWith(AppSortType.Name.comparator(ascending = true))
    }.flowOn(dispatcherProvider.default())

    var isStarted: Boolean = false
        private set

    fun startFromListFilter() {
        exportFilterRepository.update(listFilterRepository.filter.value)
        isStarted = true
    }

    fun includeAllApps() {
        exportFilterRepository.clear()
    }

    fun toggleAttribute(attribute: InventoryAttribute) {
        mutableAttributes.update { if (attribute in it) it - attribute else it + attribute }
    }

    fun toggleGroup(group: InventoryAttributeGroup) {
        toggleAttributes(InventoryAttribute.entries.filter { it.group == group })
    }

    fun toggleAllAttributes() {
        toggleAttributes(InventoryAttribute.entries)
    }

    fun selectFormat(format: InventoryFormat) {
        mutableFormat.value = format
    }

    private fun toggleAttributes(attributes: List<InventoryAttribute>) {
        mutableAttributes.update { if (it.containsAll(attributes)) it - attributes.toSet() else it + attributes }
    }
}
