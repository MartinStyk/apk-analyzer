package sk.styk.martin.apkanalyzer.core.apps.storagestats

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.os.storage.StorageManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "StorageStatsRepositoryImpl"

@Singleton
internal class StorageStatsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageStatsManager: StorageStatsManager,
    private val appOpsManager: AppOpsManager,
    private val dispatcherProvider: DispatcherProvider,
    private val applicationScope: CoroutineScope,
) : StorageStatsRepository,
    DefaultLifecycleObserver {

    private var packageNames: List<PackageName> = emptyList()

    final override val isPermissionGranted: StateFlow<Boolean>
        field = MutableStateFlow(checkPermission())

    final override val totalSizes: StateFlow<Map<PackageName, AppSize>>
        field = MutableStateFlow<Map<PackageName, AppSize>>(emptyMap())

    override fun onStart(owner: LifecycleOwner) {
        applicationScope.launch(dispatcherProvider.io()) {
            fetchTotalSizes(packageNames)
        }
    }

    private fun checkPermission(): Boolean {
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun requestTotalSizes(packageNames: List<PackageName>) {
        this.packageNames = packageNames
        applicationScope.launch(dispatcherProvider.io()) {
            fetchTotalSizes(packageNames)
        }
    }

    override suspend fun queryTotalSize(packageName: PackageName): AppSize? = totalSizes.value[packageName] ?: if (checkPermission()) {
        when (val result = queryPackageSize(UserHandle.getUserHandleForUid(Process.myUid()), packageName)) {
            is SizeQueryResult.Success -> result.size

            SizeQueryResult.UninstallRace -> null

            SizeQueryResult.PermissionRace -> null

            is SizeQueryResult.Failure -> {
                Logger.w(TAG, result.error, "Storage stats loading degraded: query failed for ${packageName.value}")
                null
            }
        }
    } else {
        null
    }

    private fun fetchTotalSizes(packageNames: List<PackageName>) {
        val hasPermission = checkPermission()
        isPermissionGranted.value = hasPermission
        if (!hasPermission) {
            Logger.w(TAG, "Storage stats loading degraded: permission missing")
            return
        }

        Logger.d(TAG, "Storage stats loading started: ${packageNames.size} apps requested")
        val user = UserHandle.getUserHandleForUid(Process.myUid())
        var uninstallRaceCount = 0
        var permissionRaceCount = 0
        var queryFailureCount = 0
        var lastQueryFailure: IOException? = null
        val sizes = packageNames.mapNotNull { packageName ->
            when (val result = queryPackageSize(user, packageName)) {
                is SizeQueryResult.Success -> packageName to result.size

                SizeQueryResult.UninstallRace -> {
                    uninstallRaceCount++
                    null
                }

                SizeQueryResult.PermissionRace -> {
                    permissionRaceCount++
                    null
                }

                is SizeQueryResult.Failure -> {
                    queryFailureCount++
                    lastQueryFailure = result.error
                    null
                }
            }
        }.toMap()
        totalSizes.value = sizes

        if (uninstallRaceCount > 0) {
            Logger.w(TAG, "Storage stats loading degraded: $uninstallRaceCount uninstalled apps skipped")
        }
        if (permissionRaceCount > 0) {
            Logger.w(TAG, "Storage stats loading degraded: $permissionRaceCount apps skipped after permission changed")
        }
        lastQueryFailure?.let {
            Logger.w(TAG, it, "Storage stats loading degraded: $queryFailureCount app queries failed")
        }
        Logger.i(TAG, "Storage stats loading finished: ${sizes.size} apps loaded")
    }

    private fun queryPackageSize(user: UserHandle, packageName: PackageName): SizeQueryResult = try {
        val stats = storageStatsManager.queryStatsForPackage(StorageManager.UUID_DEFAULT, packageName.value, user)
        SizeQueryResult.Success(stats.appBytes.bytes + stats.dataBytes.bytes + stats.cacheBytes.bytes)
    } catch (_: PackageManager.NameNotFoundException) {
        SizeQueryResult.UninstallRace
    } catch (e: IOException) {
        SizeQueryResult.Failure(e)
    } catch (_: SecurityException) {
        SizeQueryResult.PermissionRace
    }

    private sealed interface SizeQueryResult {
        data class Success(val size: AppSize) : SizeQueryResult
        data object UninstallRace : SizeQueryResult
        data object PermissionRace : SizeQueryResult
        data class Failure(val error: IOException) : SizeQueryResult
    }
}
