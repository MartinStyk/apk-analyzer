package sk.styk.martin.apkanalyzer.core.applist

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import sk.styk.martin.apkanalyzer.core.appanalysis.AppInstallSourceManager
import sk.styk.martin.apkanalyzer.core.applist.model.LazyAppListData
import java.text.Collator
import javax.inject.Inject

class InstalledAppsRepositoryImpl
@Inject
constructor(private val packageManager: PackageManager, private val appInstallSourceManager: AppInstallSourceManager) : InstalledAppsRepository {
    override fun getAll(): List<LazyAppListData> {
        val applications = packageManager.getInstalledPackages(0)
        val packages = ArrayList<LazyAppListData>(applications.size)

        applications.forEach {
            if (it.applicationInfo != null) {
                packages.add(it.toAppListData())
            }
        }

        return packages.sortedWith(comparator)
    }

    override fun getForPackageNames(packageNames: List<String>): List<LazyAppListData> {
        val packages = ArrayList<LazyAppListData>(packageNames.size)

        packageNames.forEach {
            try {
                val packageInfo = packageManager.getPackageInfo(it, 0)
                if (packageInfo?.applicationInfo != null) {
                    packages.add(packageInfo.toAppListData())
                }
            } catch (e: PackageManager.NameNotFoundException) {
                // continue
            }
        }

        return packages.sortedWith(comparator)
    }

    override fun preload(lazyAppListData: List<LazyAppListData>) {
        lazyAppListData.forEach {
            it.icon
            it.packageName
        }
    }

    private fun PackageInfo.toAppListData() = LazyAppListData(
        packageName = packageName,
        isSystemApp = appInstallSourceManager.isSystemInstalledApp(this),
        version = longVersionCode,
        source = appInstallSourceManager.getAppInstallSource(this),
        packageManager = packageManager,
        packageInfo = this,
    )

    private val comparator by lazy {
        object : Comparator<LazyAppListData> {
            private val sCollator = Collator.getInstance()

            override fun compare(object1: LazyAppListData, object2: LazyAppListData): Int = sCollator.compare(object1.applicationName, object2.applicationName)
        }
    }
}
