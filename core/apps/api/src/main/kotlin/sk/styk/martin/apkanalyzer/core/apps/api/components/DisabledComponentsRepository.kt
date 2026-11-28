package sk.styk.martin.apkanalyzer.core.apps.api.components

import sk.styk.martin.apkanalyzer.core.common.model.PackageName

interface DisabledComponentsRepository {
    suspend fun disabledComponents(packageName: PackageName): Result<DisabledComponents>
}
