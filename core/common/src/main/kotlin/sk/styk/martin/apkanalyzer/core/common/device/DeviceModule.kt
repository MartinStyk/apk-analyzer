package sk.styk.martin.apkanalyzer.core.common.device

import android.app.ActivityManager
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DeviceModule {
    @Binds
    @Singleton
    fun bindDeviceInfoProvider(impl: DeviceInfoProviderImpl): DeviceInfoProvider
}

@Module
@InstallIn(SingletonComponent::class)
internal class DevicePlatformModule {
    @Provides
    @Singleton
    fun provideActivityManager(@ApplicationContext context: Context): ActivityManager = context.getSystemService(ActivityManager::class.java)
}
