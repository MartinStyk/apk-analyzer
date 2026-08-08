package sk.styk.martin.apkanalyzer.core.apkfiles.di

import android.app.ActivityManager
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.apkfiles.TemporaryApkManager
import sk.styk.martin.apkanalyzer.core.apkfiles.TemporaryApkManagerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TemporaryApkModule {

    @Binds
    @Singleton
    fun bindTemporaryApkManager(impl: TemporaryApkManagerImpl): TemporaryApkManager

    companion object {
        @Provides
        @Singleton
        fun provideActivityManager(@ApplicationContext context: Context): ActivityManager = context.getSystemService(ActivityManager::class.java)
    }
}
