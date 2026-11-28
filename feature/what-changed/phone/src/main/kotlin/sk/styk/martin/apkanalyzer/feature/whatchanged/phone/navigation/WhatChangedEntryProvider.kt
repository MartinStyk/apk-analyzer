package sk.styk.martin.apkanalyzer.feature.whatchanged.phone.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.animation.slideFromEndEntryMetadata
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppDetailNavKey
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppHistoryChangeNavKey
import sk.styk.martin.apkanalyzer.feature.appdetail.api.AppHistoryTimelineNavKey
import sk.styk.martin.apkanalyzer.feature.pro.api.PaywallReason
import sk.styk.martin.apkanalyzer.feature.pro.api.ProNavKey
import sk.styk.martin.apkanalyzer.feature.settings.api.AppHistorySettingsNavKey
import sk.styk.martin.apkanalyzer.feature.whatchanged.api.WhatChangedNavKey
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.WhatChangedScreen
import sk.styk.martin.apkanalyzer.feature.whatchanged.phone.log.WhatChangedLogScreen

fun EntryProviderScope<NavKey>.whatChangedEntries(navigator: Navigator) {
    entry<WhatChangedNavKey> {
        WhatChangedScreen(
            onNavigateToFullHistory = { navigator.navigate(WhatChangedLogNavKey) },
            onNavigateToSettings = { navigator.navigate(AppHistorySettingsNavKey) },
            onNavigateToChange = { packageName, snapshotId, appName -> navigator.navigate(AppHistoryChangeNavKey(packageName, snapshotId, appName)) },
            onNavigateToFullDetail = { input -> navigator.navigate(AppDetailNavKey(input)) },
            onNavigateToAppHistory = { packageName, appName -> navigator.navigate(AppHistoryTimelineNavKey(packageName, appName)) },
            onNavigateToPaywall = { lockedAppCount -> navigator.navigate(ProNavKey(PaywallReason.WhatChanged(lockedAppCount))) },
        )
    }

    entry<WhatChangedLogNavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) {
        WhatChangedLogScreen(
            onBack = { navigator.goBack() },
            onNavigateToChange = { packageName, snapshotId, appName -> navigator.navigate(AppHistoryChangeNavKey(packageName, snapshotId, appName)) },
            onNavigateToFullDetail = { input -> navigator.navigate(AppDetailNavKey(input)) },
            onNavigateToPaywall = { lockedAppCount -> navigator.navigate(ProNavKey(PaywallReason.WhatChanged(lockedAppCount))) },
        )
    }
}
