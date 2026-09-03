package sk.styk.martin.apkanalyzer.core.apps

import kotlinx.coroutines.flow.Flow
import sk.styk.martin.apkanalyzer.core.common.model.PackageName

interface PackageChangesObserver {
    fun observe(): Flow<PackageChangeEvent>
}

data class PackageChangeEvent(val packageName: PackageName, val action: PackageChangeAction)

enum class PackageChangeAction {
    Added,
    Removed,
    Replaced,
}
