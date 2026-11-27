package sk.styk.martin.apkanalyzer.ui.navigation

import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.NavigationBarItem
import sk.styk.martin.apkanalyzer.feature.apps.api.AppsNavKey
import sk.styk.martin.apkanalyzer.feature.browse.api.BrowseNavKey
import sk.styk.martin.apkanalyzer.feature.whatchanged.api.WhatChangedNavKey
import sk.styk.martin.apkanalyzer.feature.apps.api.R as AppsR
import sk.styk.martin.apkanalyzer.feature.browse.api.R as BrowseR
import sk.styk.martin.apkanalyzer.feature.whatchanged.api.R as WhatChangedR

private val TOP_LEVEL_DESTINATIONS =
    listOf(
        NavigationBarItem(
            navKey = AppsNavKey,
            selectedIcon = ApkAnalyzerIcons.Apps,
            unselectedIcon = ApkAnalyzerIcons.AppsBorder,
            title = AppsR.string.apps,
        ),
        NavigationBarItem(
            navKey = BrowseNavKey,
            selectedIcon = ApkAnalyzerIcons.Browse,
            unselectedIcon = ApkAnalyzerIcons.BrowseBorder,
            title = BrowseR.string.browse,
        ),
        NavigationBarItem(
            navKey = WhatChangedNavKey,
            selectedIcon = ApkAnalyzerIcons.History,
            unselectedIcon = ApkAnalyzerIcons.HistoryBorder,
            title = WhatChangedR.string.what_changed,
        ),
    )

internal val TOP_LEVEL_KEYS: List<NavKey> = TOP_LEVEL_DESTINATIONS.map { it.navKey }

internal fun topLevelDestinations(isWhatChangedEnabled: Boolean): List<NavigationBarItem> =
    TOP_LEVEL_DESTINATIONS.filter { isWhatChangedEnabled || it.navKey != WhatChangedNavKey }
