package sk.styk.martin.apkanalyzer.core.appanalysis.model

import android.content.pm.PackageInfo

enum class InstallLocation {
    AUTO,
    INTERNAL,
    EXTERNAL,
    ;

    companion object {
        fun from(installLocation: Int): InstallLocation = when (installLocation) {
            PackageInfo.INSTALL_LOCATION_AUTO -> AUTO
            PackageInfo.INSTALL_LOCATION_INTERNAL_ONLY -> INTERNAL
            PackageInfo.INSTALL_LOCATION_PREFER_EXTERNAL -> EXTERNAL
            else -> INTERNAL
        }
    }
}
