package sk.styk.martin.apkanalyzer.core.apps.model

import android.content.pm.PackageInfo

enum class InstallLocation {
    Auto,
    Internal,
    External,
    ;

    companion object {
        fun from(installLocation: Int): InstallLocation = when (installLocation) {
            PackageInfo.INSTALL_LOCATION_AUTO -> Auto
            PackageInfo.INSTALL_LOCATION_INTERNAL_ONLY -> Internal
            PackageInfo.INSTALL_LOCATION_PREFER_EXTERNAL -> External
            else -> Internal
        }
    }
}
