package sk.styk.martin.apkanalyzer.feature.whatchanged.phone.access

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryChangedApp
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryFreeSample
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.isFreeAt
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.isFreeSample
import sk.styk.martin.apkanalyzer.core.entitlements.api.Entitlement
import sk.styk.martin.apkanalyzer.core.entitlements.api.EntitlementsRepository
import javax.inject.Inject

internal class WhatChangedAccessPolicy @Inject constructor(private val entitlementsRepository: EntitlementsRepository) {

    fun hasPro(): Flow<Boolean> = entitlementsRepository.entitlements().map { Entitlement.Pro in it }.distinctUntilChanged()

    fun accessTo(hasPro: Boolean): WhatChangedAccess = WhatChangedAccess(hasPro)
}

internal class WhatChangedAccess(private val hasPro: Boolean) {

    fun canReadSectionRow(index: Int): Boolean = hasPro || index < AppHistoryFreeSample.SECTION_ROWS

    fun canReadSectionRow(index: Int, entry: AppHistoryEntry): Boolean = canReadSectionRow(index) || entry.isFreeSample

    fun canReadSectionRow(index: Int, app: AppHistoryChangedApp): Boolean = hasPro || app.isFreeAt(index)

    fun canReadLogRow(index: Int, entry: AppHistoryEntry): Boolean = hasPro || index < AppHistoryFreeSample.LOG_ROWS || entry.isFreeSample
}
