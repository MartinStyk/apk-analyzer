package sk.styk.martin.apkanalyzer.feature.whatchanged.phone.access

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryChangedApp
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryDeviceTimeline
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.AppHistoryEntry
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.hasCertificateChange
import sk.styk.martin.apkanalyzer.core.apphistory.api.timeline.model.latestChange
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.entitlements.api.Entitlement
import sk.styk.martin.apkanalyzer.core.entitlements.api.EntitlementsRepository
import javax.inject.Inject

internal class WhatChangedAccessPolicy @Inject constructor(private val entitlementsRepository: EntitlementsRepository) {

    fun hasPro(): Flow<Boolean> = entitlementsRepository.entitlements().map { Entitlement.Pro in it }.distinctUntilChanged()

    fun accessTo(timeline: AppHistoryDeviceTimeline.Available, hasPro: Boolean): WhatChangedAccess =
        WhatChangedAccess(hasPro = hasPro, sampleApp = timeline.latestChange?.packageName)
}

internal class WhatChangedAccess(private val hasPro: Boolean, private val sampleApp: PackageName?) {

    private val AppHistoryChangedApp.isInstallOnly: Boolean
        get() = isNew && updateCount == 0 && netChange == null

    fun canReadApp(packageName: PackageName): Boolean = hasPro || packageName == sampleApp

    fun canRead(packageName: PackageName, entry: AppHistoryEntry): Boolean = canReadApp(packageName) ||
        when (entry) {
            is AppHistoryEntry.FirstSeen -> true
            is AppHistoryEntry.Change -> entry.diff.hasCertificateChange
        }

    fun canRead(app: AppHistoryChangedApp): Boolean = canReadApp(app.packageName) ||
        app.netChange?.hasCertificateChange == true ||
        app.isInstallOnly
}
