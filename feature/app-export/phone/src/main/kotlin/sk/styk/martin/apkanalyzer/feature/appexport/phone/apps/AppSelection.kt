package sk.styk.martin.apkanalyzer.feature.appexport.phone.apps

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.filtering.AppFilterRepositories
import sk.styk.martin.apkanalyzer.core.apps.filtering.AppFilterScope
import sk.styk.martin.apkanalyzer.core.apps.filtering.AppFilterState
import sk.styk.martin.apkanalyzer.core.apps.filtering.filterBy
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.sorting.AppSortType
import sk.styk.martin.apkanalyzer.core.apps.sorting.comparator
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportScope

internal val InventoryExportFilterScope = AppFilterScope("inventory-export")

internal class AppSelection(
    exportScope: InventoryExportScope,
    installedAppsRepository: InstalledAppsRepository,
    appFilterRepositories: AppFilterRepositories,
    dispatcherProvider: DispatcherProvider,
    scope: CoroutineScope,
) {
    private val filter: Flow<AppFilterState> = when (exportScope) {
        InventoryExportScope.InstalledApps -> appFilterRepositories.forScope(InventoryExportFilterScope).also {
            it.update(appFilterRepositories.forScope(AppFilterScope.List).filter.value)
        }.filter

        is InventoryExportScope.SingleApp -> flowOf(AppFilterState())
    }
    private val installedApps: Flow<List<InstalledApp>> = when (exportScope) {
        InventoryExportScope.InstalledApps -> installedAppsRepository.apps()

        is InventoryExportScope.SingleApp -> installedAppsRepository.apps().map { apps ->
            apps.filter { it.packageName == exportScope.packageName }
        }
    }
    private val query = MutableStateFlow("")
    private val excludedApps = MutableStateFlow(emptySet<PackageName>())

    private val candidateApps: StateFlow<List<InstalledApp>?> = combine(
        installedApps,
        filter,
    ) { apps, filter ->
        apps.filterBy(filter).sortedWith(AppSortType.Name.comparator(ascending = true))
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(scope, SharingStarted.Eagerly, null)

    val selectedApps: StateFlow<List<InstalledApp>?> = combine(candidateApps, excludedApps) { candidates, excluded ->
        candidates?.filterNot { it.packageName in excluded }
    }
        .flowOn(dispatcherProvider.default())
        .stateIn(scope, SharingStarted.Eagerly, null)

    val state: Flow<AppSelectionState> = combine(
        candidateApps,
        excludedApps,
        query,
        filter,
    ) { candidates, excluded, query, filter ->
        AppSelectionState(
            summary = summaryOf(candidates, excluded, filter),
            items = candidates?.let { InventoryAppList.Content(it.visibleFor(query).map { app -> app.toItem(excluded) }) } ?: InventoryAppList.Loading,
            query = query,
        )
    }.flowOn(dispatcherProvider.default())

    fun search(text: String) {
        query.value = text
    }

    fun toggle(packageName: PackageName) {
        excludedApps.update { if (packageName in it) it - packageName else it + packageName }
    }

    fun toggleVisible() {
        val visible = candidateApps.value?.visibleFor(query.value)?.map { it.packageName } ?: return
        excludedApps.update { excluded ->
            if (visible.none { it in excluded }) excluded + visible else excluded - visible.toSet()
        }
    }

    private fun summaryOf(
        candidates: List<InstalledApp>?,
        excluded: Set<PackageName>,
        filter: AppFilterState,
    ) = AppSelectionSummary(
        isLoaded = candidates != null,
        candidateCount = candidates?.size ?: 0,
        selectedCount = candidates?.count { it.packageName !in excluded } ?: 0,
        isFilterActive = filter.isActive,
    )
}

private fun InstalledApp.toItem(excluded: Set<PackageName>) = InventoryAppItem(packageName, applicationName, isSelected = packageName !in excluded)

private fun List<InstalledApp>.visibleFor(query: String): List<InstalledApp> = filter { it.matches(query) }

private fun InstalledApp.matches(query: String): Boolean =
    query.isBlank() || applicationName.contains(query.trim(), ignoreCase = true) || packageName.value.contains(query.trim(), ignoreCase = true)
