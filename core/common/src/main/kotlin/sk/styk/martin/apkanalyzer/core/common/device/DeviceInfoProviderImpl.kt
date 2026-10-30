package sk.styk.martin.apkanalyzer.core.common.device

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DeviceInfoProviderImpl @Inject constructor(@ApplicationContext private val context: Context) : DeviceInfoProvider {

    @delegate:SuppressLint("HardwareIds")
    override val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
    }

    override val manufacturer: String = Build.MANUFACTURER

    override val model: String = Build.MODEL
}
