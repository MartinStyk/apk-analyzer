package sk.styk.martin.apkanalyzer.core.apps.impl.model

import android.content.pm.PackageInfo
import sk.styk.martin.apkanalyzer.core.apps.api.model.InstallLocation

internal fun resolveInstallLocation(installLocation: Int): InstallLocation = when (installLocation) {
    PackageInfo.INSTALL_LOCATION_AUTO -> InstallLocation.Auto
    PackageInfo.INSTALL_LOCATION_INTERNAL_ONLY -> InstallLocation.Internal
    PackageInfo.INSTALL_LOCATION_PREFER_EXTERNAL -> InstallLocation.External
    else -> InstallLocation.Internal
}
