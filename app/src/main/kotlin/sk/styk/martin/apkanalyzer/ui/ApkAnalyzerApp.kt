package sk.styk.martin.apkanalyzer.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.core.navigation.rememberNavigationState
import sk.styk.martin.apkanalyzer.core.navigation.toEntries
import sk.styk.martin.apkanalyzer.core.uilibrary.components.NavigationBar
import sk.styk.martin.apkanalyzer.feature.apps.api.AppsNavKey
import sk.styk.martin.apkanalyzer.feature.apps.impl.navigation.appEntries
import sk.styk.martin.apkanalyzer.feature.permissions.impl.navigation.permissionEntries
import sk.styk.martin.apkanalyzer.feature.statistics.impl.navigation.statisticsEntries
import sk.styk.martin.apkanalyzer.ui.navigation.TOP_LEVEL_DESTINATIONS
import sk.styk.martin.apkanalyzer.ui.navigation.TOP_LEVEL_KEYS

@Composable
internal fun ApkAnalyzerApp() {
    val navigationState =
        rememberNavigationState(
            startKey = AppsNavKey,
            topLevelKeys = TOP_LEVEL_KEYS,
        )
    val navigator =
        remember {
            Navigator(navigationState)
        }

    Scaffold(
        bottomBar = {
            NavigationBar(
                items = TOP_LEVEL_DESTINATIONS,
                selectedKey = navigationState.topLevelKey,
                onSelectKey = navigator::navigate,
            )
        },
    ) { paddings ->
        val entryProvider =
            entryProvider {
                appEntries()
                permissionEntries()
                statisticsEntries()
            }
        NavDisplay(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(paddings),
            entries = navigationState.toEntries(entryProvider),
            onBack = navigator::goBack,
        )
    }
}
