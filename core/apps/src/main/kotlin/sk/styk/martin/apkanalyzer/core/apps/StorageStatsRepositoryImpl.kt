package sk.styk.martin.apkanalyzer.core.apps

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
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class StorageStatsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageStatsManager: StorageStatsManager,
    private val appOpsManager: AppOpsManager,
    private val dispatcherProvider: DispatcherProvider,
    private val applicationScope: CoroutineScope,
) : StorageStatsRepository,
    DefaultLifecycleObserver {

    private var packageNames: List<String> = emptyList()

    final override val isPermissionGranted: StateFlow<Boolean>
        field = MutableStateFlow(checkPermission())

    final override val totalSizes: StateFlow<Map<String, AppSize>>
        field = MutableStateFlow<Map<String, AppSize>>(emptyMap())

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

    override fun requestTotalSizes(packageNames: List<String>) {
        this.packageNames = packageNames
        applicationScope.launch(dispatcherProvider.io()) {
            fetchTotalSizes(packageNames)
        }
    }

    override suspend fun queryTotalSize(packageName: String): AppSize? {
        totalSizes.value[packageName]?.let { return it }
        if (!checkPermission()) return null
        return queryPackageSize(UserHandle.getUserHandleForUid(Process.myUid()), packageName)
    }

    private fun fetchTotalSizes(packageNames: List<String>) {
        val hasPermission = checkPermission()
        isPermissionGranted.value = hasPermission
        if (!hasPermission) return

        Logger.d(INSTALLED_APPS, "Load apps total size")
        val user = UserHandle.getUserHandleForUid(Process.myUid())
        totalSizes.value = packageNames.mapNotNull { packageName ->
            queryPackageSize(user, packageName)?.let { packageName to it }
        }.toMap()
        Logger.d(INSTALLED_APPS, "Apps total size loaded")
    }

    private fun queryPackageSize(user: UserHandle, packageName: String): AppSize? = try {
        val stats = storageStatsManager.queryStatsForPackage(StorageManager.UUID_DEFAULT, packageName, user)
        stats.appBytes.bytes + stats.dataBytes.bytes + stats.cacheBytes.bytes
    } catch (_: PackageManager.NameNotFoundException) {
        null
    } catch (_: IOException) {
        Logger.w(TAG, "Failed to query storage stats for $packageName")
        null
    } catch (_: SecurityException) {
        null
    }

    private companion object {
        const val TAG = "StorageStatsRepository"
    }
}
