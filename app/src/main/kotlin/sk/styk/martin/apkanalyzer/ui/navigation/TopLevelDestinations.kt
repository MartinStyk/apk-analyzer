package sk.styk.martin.apkanalyzer.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.uilibrary.components.NavigationBarItem
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.feature.apps.api.AppsNavKey
import sk.styk.martin.apkanalyzer.feature.permissions.api.PermissionsNavKey
import sk.styk.martin.apkanalyzer.feature.statistics.api.StatisticsNavKey
import sk.styk.martin.apkanalyzer.feature.apps.api.R as AppsR
import sk.styk.martin.apkanalyzer.feature.permissions.api.R as PermissionsR
import sk.styk.martin.apkanalyzer.feature.statistics.api.R as StatisticsR

internal val TOP_LEVEL_DESTINATIONS =
    persistentListOf(
        NavigationBarItem(
            navKey = AppsNavKey,
            selectedIcon = ApkAnalyzerIcons.Apps,
            unselectedIcon = ApkAnalyzerIcons.AppsBorder,
            title = AppsR.string.apps,
        ),
        NavigationBarItem(
            navKey = PermissionsNavKey,
            selectedIcon = ApkAnalyzerIcons.Permissions,
            unselectedIcon = ApkAnalyzerIcons.PermissionsBorder,
            title = PermissionsR.string.permissions,
        ),
        NavigationBarItem(
            navKey = StatisticsNavKey,
            selectedIcon = ApkAnalyzerIcons.Statistics,
            unselectedIcon = ApkAnalyzerIcons.StatisticsBorder,
            title = StatisticsR.string.statistics,
        ),
    )

internal val TOP_LEVEL_KEYS: List<NavKey> = TOP_LEVEL_DESTINATIONS.map { it.navKey }
