package sk.styk.martin.apkanalyzer.feature.whatchanged.phone.access

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryChangedApp
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.hasCertificateWarning
import sk.styk.martin.apkanalyzer.core.entitlements.api.Entitlement
import sk.styk.martin.apkanalyzer.core.entitlements.api.EntitlementsRepository
import javax.inject.Inject

internal class WhatChangedAccessPolicy @Inject constructor(private val entitlementsRepository: EntitlementsRepository) {

    fun hasPro(): Flow<Boolean> = entitlementsRepository.entitlements().map { Entitlement.Pro in it }.distinctUntilChanged()

    fun accessTo(hasPro: Boolean): WhatChangedAccess = WhatChangedAccess(hasPro)
}

internal class WhatChangedAccess(private val hasPro: Boolean) {

    fun canReadSectionRow(index: Int): Boolean = hasPro || index < SECTION_PREVIEW_COUNT

    fun canReadSectionRow(index: Int, entry: AppHistoryEntry): Boolean = canReadSectionRow(index) || isFreeEntry(entry)

    fun canReadSectionRow(index: Int, app: AppHistoryChangedApp): Boolean = canReadSectionRow(index) || app.hasCertificateWarning

    fun canReadLogRow(index: Int, entry: AppHistoryEntry): Boolean = hasPro || index < LOG_PREVIEW_COUNT || isFreeEntry(entry)

    private fun isFreeEntry(entry: AppHistoryEntry): Boolean = when (entry) {
        is AppHistoryEntry.FirstSeen -> true
        is AppHistoryEntry.Change -> entry.hasCertificateWarning
    }
}

private const val SECTION_PREVIEW_COUNT = 5
private const val LOG_PREVIEW_COUNT = 10
