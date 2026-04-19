package sk.styk.martin.apkanalyzer.feature.statistics.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.feature.statistics.api.StatisticsNavKey
import sk.styk.martin.apkanalyzer.feature.statistics.impl.StatisticsScreen

fun EntryProviderScope<NavKey>.statisticsEntries() {
    entry<StatisticsNavKey> {
        StatisticsScreen()
    }
}
