package sk.styk.martin.apkanalyzer.core.apps.impl.components

import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.api.components.DisabledComponents
import sk.styk.martin.apkanalyzer.core.apps.api.components.DisabledComponentsRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import sk.styk.martin.apkanalyzer.core.common.performance.TraceOutcome
import sk.styk.martin.apkanalyzer.core.common.performance.outcome
import sk.styk.martin.apkanalyzer.core.common.performance.startCancellableTrace
import sk.styk.martin.apkanalyzer.core.common.performance.timedSection
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DisabledComponentsRepositoryImpl"

private const val COMPONENT_FLAGS = PackageManager.GET_ACTIVITIES or
    PackageManager.GET_SERVICES or
    PackageManager.GET_RECEIVERS or
    PackageManager.GET_PROVIDERS

@Singleton
internal class DisabledComponentsRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val dispatcherProvider: DispatcherProvider,
    private val performanceTracker: PerformanceTracker,
) : DisabledComponentsRepository {

    override suspend fun disabledComponents(packageName: PackageName): Result<DisabledComponents> =
        performanceTracker.startCancellableTrace("disabled_components_load") {
            val context = "package=${packageName.value}"
            runCatchingCancellable {
                withContext(dispatcherProvider.io()) {
                    timedSection(TAG, "Disabled components package query", "package_query_ms", context) {
                        val declared = packageManager.getPackageInfo(packageName.value, COMPONENT_FLAGS or PackageManager.MATCH_DISABLED_COMPONENTS)
                        val enabled = packageManager.getPackageInfo(packageName.value, COMPONENT_FLAGS)
                        DisabledComponents(
                            activities = declared.activities.names() - enabled.activities.names(),
                            services = declared.services.names() - enabled.services.names(),
                            receivers = declared.receivers.names() - enabled.receivers.names(),
                            providers = declared.providers.names() - enabled.providers.names(),
                        )
                    }
                }
            }.onSuccess {
                outcome = TraceOutcome.Success
                Logger.i(TAG, "Disabled components loading finished: ${it.count} disabled, $context")
            }.onFailure {
                outcome = TraceOutcome.Error
                Logger.e(TAG, it, "Disabled components loading failed: $context")
            }
        }
}

private fun Array<out ComponentInfo>?.names(): Set<String> = orEmpty().map { it.name }.toSet()

private val DisabledComponents.count: Int
    get() = activities.size + services.size + receivers.size + providers.size
