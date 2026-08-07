package sk.styk.martin.apkanalyzer.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.uilibrary.components.NavigationBarItem
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.feature.apps.api.AppsNavKey
import sk.styk.martin.apkanalyzer.feature.browse.api.BrowseNavKey
import sk.styk.martin.apkanalyzer.feature.apps.api.R as AppsR
import sk.styk.martin.apkanalyzer.feature.browse.api.R as BrowseR

internal val TOP_LEVEL_DESTINATIONS =
    persistentListOf(
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
    )

internal val TOP_LEVEL_KEYS: List<NavKey> = TOP_LEVEL_DESTINATIONS.map { it.navKey }
