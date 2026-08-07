package sk.styk.martin.apkanalyzer.core.appindex

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import sk.styk.martin.apkanalyzer.core.appindex.model.AppIndexStatus
import sk.styk.martin.apkanalyzer.core.apps.AppSigningRepository
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import javax.inject.Inject

internal class AppIndexRepositoryImpl @Inject constructor(private val installedAppsRepository: InstalledAppsRepository, private val appSigningRepository: AppSigningRepository, private val dispatcherProvider: DispatcherProvider) :
    AppIndexRepository {

    override fun index(): Flow<AppIndexStatus> = combine(
        installedAppsRepository.apps(),
        appSigningRepository.signing(),
    ) { apps, signing -> AppIndexStatus.Data(AppIndexer.index(apps, signing)) as AppIndexStatus }
        .onStart { emit(AppIndexStatus.Loading) }
        .flowOn(dispatcherProvider.default())
}
