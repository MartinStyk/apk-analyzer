package sk.styk.martin.apkanalyzer.core.apps.analysis

import sk.styk.martin.apkanalyzer.core.apps.R
import sk.styk.martin.apkanalyzer.core.common.resources.ResourcesManager
import javax.inject.Inject

const val MAX_SDK_VERSION = 37

class SdkVersionResolver @Inject constructor(private val resourcesManager: ResourcesManager) {
    fun resolveVersion(sdkVersion: Int?): String? = sdkVersion?.let { sdk ->
        val versions = resourcesManager.getStringArray(R.array.android_versions)
        versions.getOrNull(sdk - 1) ?: "Android ${sdk - 20}"
    }
}
