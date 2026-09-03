package sk.styk.martin.apkanalyzer.core.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import javax.inject.Inject

class PackageChangesObserverImpl @Inject constructor(@ApplicationContext private val context: Context, appScope: CoroutineScope) : PackageChangesObserver {

    private val events = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Logger.d(INSTALLED_APPS, "Received package change event $intent")
                val event = intent.toPackageChangeEvent()
                if (event == null) {
                    Logger.w(INSTALLED_APPS, "Package change broadcast with unparsable data or action: ${intent.action}")
                    return
                }
                Logger.i(INSTALLED_APPS, "Package change detected: $event")
                trySend(event)
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        context.registerReceiver(receiver, filter)
        awaitClose { context.unregisterReceiver(receiver) }
    }.shareIn(
        scope = appScope,
        started = SharingStarted.WhileSubscribed(),
        replay = 0,
    )

    override fun observe(): Flow<PackageChangeEvent> = events

    private fun Intent.toPackageChangeEvent(): PackageChangeEvent? {
        val packageName = data?.schemeSpecificPart ?: return null
        val action = when (action) {
            Intent.ACTION_PACKAGE_ADDED -> PackageChangeAction.Added
            Intent.ACTION_PACKAGE_REMOVED -> PackageChangeAction.Removed
            Intent.ACTION_PACKAGE_REPLACED -> PackageChangeAction.Replaced
            else -> return null
        }
        return PackageChangeEvent(PackageName(packageName), action)
    }
}
