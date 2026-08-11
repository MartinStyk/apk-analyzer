package sk.styk.martin.apkanalyzer.core.apps.storagestats

import sk.styk.martin.apkanalyzer.core.common.model.AppSize

data class StorageBreakdown(
    val appBytes: AppSize,
    val dataBytes: AppSize,
    val cacheBytes: AppSize,
) {
    val total: AppSize get() = appBytes + dataBytes + cacheBytes
}
