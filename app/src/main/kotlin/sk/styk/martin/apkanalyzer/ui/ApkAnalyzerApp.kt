package sk.styk.martin.apkanalyzer.ui

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.flow.distinctUntilChanged
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.core.navigation.ScreenOpenEvent
import sk.styk.martin.apkanalyzer.core.navigation.rememberNavigationState
import sk.styk.martin.apkanalyzer.core.navigation.toEntries
import sk.styk.martin.apkanalyzer.core.uilibrary.components.NavigationBar
import sk.styk.martin.apkanalyzer.core.uilibrary.modifier.LocalSharedTransitionScope
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.navigation.appDetailEntries
import sk.styk.martin.apkanalyzer.feature.apps.api.AppsNavKey
import sk.styk.martin.apkanalyzer.feature.apps.impl.navigation.appEntries
import sk.styk.martin.apkanalyzer.feature.browse.impl.navigation.browseEntries
import sk.styk.martin.apkanalyzer.feature.settings.impl.navigation.settingsEntries
import sk.styk.martin.apkanalyzer.ui.navigation.TOP_LEVEL_DESTINATIONS
import sk.styk.martin.apkanalyzer.ui.navigation.TOP_LEVEL_KEYS
import sk.styk.martin.apkanalyzer.ui.navigation.logScreenOpened
import sk.styk.martin.apkanalyzer.feature.appdetail.impl.navigation.screenOpenEvent as appDetailScreenOpenEvent
import sk.styk.martin.apkanalyzer.feature.apps.impl.navigation.screenOpenEvent as appsScreenOpenEvent
import sk.styk.martin.apkanalyzer.feature.browse.impl.navigation.screenOpenEvent as browseScreenOpenEvent
import sk.styk.martin.apkanalyzer.feature.settings.impl.navigation.screenOpenEvent as settingsScreenOpenEvent

@Composable
internal fun ApkAnalyzerApp() {
    val navigationState = rememberNavigationState(
        startKey = AppsNavKey,
        topLevelKeys = TOP_LEVEL_KEYS,
    )
    val navigator = remember {
        Navigator(navigationState)
    }

    LaunchedEffect(navigationState) {
        snapshotFlow { navigationState.currentKey }
            .distinctUntilChanged()
            .collect { key -> logScreenOpened(key, ::resolveScreenOpenEvent) }
    }

    Scaffold { paddings ->
        Box(modifier = Modifier.fillMaxSize()) {
            SharedTransitionLayout {
                CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                    val entryProvider = entryProvider {
                        appEntries(navigator)
                        appDetailEntries(navigator)
                        browseEntries(navigator)
                        settingsEntries(navigator)
                    }
                    NavDisplay(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(paddings),
                        entries = navigationState.toEntries(entryProvider),
                        onBack = navigator::goBack,
                    )
                }
            }
            NavigationBar(
                items = TOP_LEVEL_DESTINATIONS,
                selectedKey = navigationState.currentTopLevelKey,
                isVisible = navigationState.currentKey in navigationState.topLevelKeys,
                onSelectKey = navigator::navigate,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun resolveScreenOpenEvent(key: NavKey): ScreenOpenEvent? = appsScreenOpenEvent(key)
    ?: appDetailScreenOpenEvent(key)
    ?: browseScreenOpenEvent(key)
    ?: settingsScreenOpenEvent(key)
