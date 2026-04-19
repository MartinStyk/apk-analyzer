package sk.styk.martin.apkanalyzer.core.applist

import sk.styk.martin.apkanalyzer.core.applist.model.LazyAppListData

interface InstalledAppsRepository {
    fun getAll(): List<LazyAppListData>

    fun getForPackageNames(packageNames: List<String>): List<LazyAppListData>

    fun preload(lazyAppListData: List<LazyAppListData>)
}
