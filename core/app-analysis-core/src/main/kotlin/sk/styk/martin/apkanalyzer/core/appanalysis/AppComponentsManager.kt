package sk.styk.martin.apkanalyzer.core.appanalysis

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import sk.styk.martin.apkanalyzer.core.appanalysis.model.ActivityData
import sk.styk.martin.apkanalyzer.core.appanalysis.model.BroadcastReceiverData
import sk.styk.martin.apkanalyzer.core.appanalysis.model.ContentProviderData
import sk.styk.martin.apkanalyzer.core.appanalysis.model.ServiceData
import javax.inject.Inject

class AppComponentsManager
@Inject
constructor(private val packageManager: PackageManager) {
    fun getActivities(packageInfo: PackageInfo): List<ActivityData> = packageInfo.activities.orEmpty().map {
        ActivityData(
            name = it.name,
            packageName = it.packageName,
            label = it.loadLabel(packageManager).toString(),
            targetActivity = it.targetActivity,
            permission = it.permission,
            parentName = it.parentActivityName,
            isExported = it.exported,
        )
    }

    fun getServices(packageInfo: PackageInfo): List<ServiceData> = packageInfo.services.orEmpty().map {
        ServiceData(
            name = it.name,
            permission = it.permission,
            isExported = it.exported,
            isStopWithTask = it.flags and ServiceInfo.FLAG_STOP_WITH_TASK > 0,
            isSingleUser = it.flags and ServiceInfo.FLAG_SINGLE_USER > 0,
            isIsolatedProcess = it.flags and ServiceInfo.FLAG_ISOLATED_PROCESS > 0,
            isExternalService = it.flags and ServiceInfo.FLAG_EXTERNAL_SERVICE > 0,
        )
    }

    fun getContentProviders(packageInfo: PackageInfo): List<ContentProviderData> = packageInfo.providers.orEmpty().map {
        ContentProviderData(
            name = it.name,
            authority = it.authority,
            readPermission = it.readPermission,
            writePermission = it.writePermission,
            isExported = it.exported,
        )
    }

    fun getBroadcastReceivers(packageInfo: PackageInfo): List<BroadcastReceiverData> = packageInfo.receivers.orEmpty().map {
        BroadcastReceiverData(
            name = it.name,
            permission = it.permission,
            isExported = it.exported,
        )
    }
}
