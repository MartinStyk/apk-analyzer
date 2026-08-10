package sk.styk.martin.apkanalyzer.ui.navigation

import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.navigation.ScreenOpenEvent

private const val TAG = "Navigation"
private const val OPERATION = "navigation"

internal fun logScreenOpened(key: NavKey, resolveScreenOpenEvent: (NavKey) -> ScreenOpenEvent?) {
    val event = resolveScreenOpenEvent(key) ?: return
    val message = if (event.context != null) {
        "operation=$OPERATION event=screen_opened screen=${event.screen} ${event.context}"
    } else {
        "operation=$OPERATION event=screen_opened screen=${event.screen}"
    }
    Logger.i(TAG, message)
}
