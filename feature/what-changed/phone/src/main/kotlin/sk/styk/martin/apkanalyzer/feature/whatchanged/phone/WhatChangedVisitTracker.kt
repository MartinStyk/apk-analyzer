package sk.styk.martin.apkanalyzer.feature.whatchanged.phone

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WhatChangedVisitTracker @Inject constructor(private val persistenceRepository: PersistenceRepository) {
    private val mutex = Mutex()
    private var previousVisit: Instant? = null
    private var isVisitRecorded = false

    suspend fun previousVisit(): Instant? = mutex.withLock {
        if (!isVisitRecorded) {
            previousVisit = persistenceRepository.get(Key.WhatChangedLastVisitAt).takeIf { it > 0 }?.let(Instant::ofEpochMilli)
            persistenceRepository.save(Key.WhatChangedLastVisitAt, Instant.now().toEpochMilli())
            isVisitRecorded = true
        }
        previousVisit
    }
}
