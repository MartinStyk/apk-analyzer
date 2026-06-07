package sk.styk.martin.apkanalyzer.core.apps

import kotlinx.coroutines.flow.Flow

interface PackageChangesObserver {
    fun observe(): Flow<Unit>
}
