package sk.styk.martin.apkanalyzer.core.appanalysis

import sk.styk.martin.apkanalyzer.core.common.resources.ResourcesManager
import javax.inject.Inject

const val MAX_SDK_VERSION = 35

class AndroidVersionManager
@Inject
constructor(private val resourcesManager: ResourcesManager) {
    fun resolveVersion(sdkVersion: Int?): String? {
        if (sdkVersion == null) return null
        // java index from 0 - first item is sdk 1
        val index = sdkVersion - 1

        val versions = resourcesManager.getStringArray(R.array.android_versions)

        return if (index >= 0 && index < versions.size) versions[index] else null
    }
}
