package sk.styk.martin.apkanalyzer.feature.pro.phone.support

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

internal data class InstallDetails(val versionCode: Long, val installedAt: Instant)

internal class AppInstallInfo @Inject constructor(@param:ApplicationContext private val context: Context) {

    fun read(): InstallDetails {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return InstallDetails(
            versionCode = packageInfo.longVersionCode,
            installedAt = Instant.ofEpochMilli(packageInfo.firstInstallTime),
        )
    }
}
