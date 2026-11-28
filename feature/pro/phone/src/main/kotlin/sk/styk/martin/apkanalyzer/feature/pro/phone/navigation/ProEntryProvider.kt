package sk.styk.martin.apkanalyzer.feature.pro.phone.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.designsystem.phone.navigation.BottomSheetSceneStrategy
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.feature.pro.api.ProNavKey
import sk.styk.martin.apkanalyzer.feature.pro.phone.ProScreen

fun EntryProviderScope<NavKey>.proEntries(navigator: Navigator) {
    entry<ProNavKey>(metadata = BottomSheetSceneStrategy.bottomSheet()) { key ->
        ProScreen(
            reason = key.reason,
            onClose = { navigator.goBack() },
            onCloseWithResult = { result -> navigator.goBack(result) },
        )
    }
}
