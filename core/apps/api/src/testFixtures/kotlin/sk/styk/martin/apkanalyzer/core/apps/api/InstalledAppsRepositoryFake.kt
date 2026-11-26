package sk.styk.martin.apkanalyzer.core.apps

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

class InstalledAppsRepositoryFake(vararg apps: InstalledApp) : InstalledAppsRepository {
    private val apps = MutableStateFlow(apps.associateBy { it.packageName })

    operator fun plusAssign(app: InstalledApp) {
        apps.update { it + (app.packageName to app) }
    }

    override fun apps(): Flow<List<InstalledApp>> = apps.map { it.values.toList() }

    override suspend fun awaitFullyEnrichedApps(): List<InstalledApp> = apps.value.values.toList()

    override suspend fun app(packageName: PackageName): InstalledApp? = apps.value[packageName]
}
