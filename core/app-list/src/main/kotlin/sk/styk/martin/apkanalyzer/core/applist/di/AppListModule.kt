package sk.styk.martin.apkanalyzer.core.applist.di

import android.app.Application
import android.content.pm.PackageManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.applist.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.applist.InstalledAppsRepositoryImpl
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface AppListModule {
    @Binds
    @Singleton
    fun bindInstalledAppsRepository(installedAppsRepositoryImpl: InstalledAppsRepositoryImpl): InstalledAppsRepository
}
