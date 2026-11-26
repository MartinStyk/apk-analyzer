package sk.styk.martin.apkanalyzer.core.apps.impl.filtering

import sk.styk.martin.apkanalyzer.core.apps.api.filtering.AppFilterRepositories
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.AppFilterRepository
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.AppFilterScope
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class AppFilterRepositoriesImpl @Inject constructor(private val listRepository: AppFilterRepository) : AppFilterRepositories {

    private val scopedRepositories = ConcurrentHashMap<AppFilterScope, AppFilterRepository>()

    override fun forScope(scope: AppFilterScope): AppFilterRepository = if (scope == AppFilterScope.List) {
        listRepository
    } else {
        scopedRepositories.getOrPut(scope) { AppFilterRepositoryImpl() }
    }
}
