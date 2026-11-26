package sk.styk.martin.apkanalyzer.core.apps.sdkversion

interface SdkVersionResolver {
    fun resolveVersion(sdkVersion: Int?): String?
}
