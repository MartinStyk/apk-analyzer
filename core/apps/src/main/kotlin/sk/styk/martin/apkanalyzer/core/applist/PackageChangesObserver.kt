package sk.styk.martin.apkanalyzer.core.applist

import kotlinx.coroutines.flow.Flow

interface PackageChangesObserver {
    fun observe(): Flow<Unit>
}
