package sk.styk.martin.apkanalyzer.core.applist.di

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import sk.styk.martin.apkanalyzer.core.applist.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.applist.InstalledAppsRepositoryImpl
import sk.styk.martin.apkanalyzer.core.applist.PackageChangesObserver
import sk.styk.martin.apkanalyzer.core.applist.PackageChangesObserverImpl
import sk.styk.martin.apkanalyzer.core.applist.RecentlyViewedAppsRepository
import sk.styk.martin.apkanalyzer.core.applist.RecentlyViewedAppsRepositoryImpl
import sk.styk.martin.apkanalyzer.core.applist.SearchHistoryRepository
import sk.styk.martin.apkanalyzer.core.applist.SearchHistoryRepositoryImpl
import sk.styk.martin.apkanalyzer.core.applist.StorageStatsRepository
import sk.styk.martin.apkanalyzer.core.applist.StorageStatsRepositoryImpl
import sk.styk.martin.apkanalyzer.core.applist.UsageStatsRepository
import sk.styk.martin.apkanalyzer.core.applist.UsageStatsRepositoryImpl
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal interface AppListModule {
    @Binds
    @Singleton
    fun bindInstalledAppsRepository(impl: InstalledAppsRepositoryImpl): InstalledAppsRepository

    @Binds
    @Singleton
    fun bindPackageChangesObserver(impl: PackageChangesObserverImpl): PackageChangesObserver

    @Binds
    @Singleton
    fun bindRecentsRepository(impl: RecentlyViewedAppsRepositoryImpl): RecentlyViewedAppsRepository

    @Binds
    @Singleton
    fun bindSearchHistoryRepository(impl: SearchHistoryRepositoryImpl): SearchHistoryRepository

    @Binds
    @Singleton
    fun bindUsageStatsRepository(impl: UsageStatsRepositoryImpl): UsageStatsRepository

    @Binds
    @IntoSet
    @Singleton
    fun bindUsageStatsAsLifecycleObserver(impl: UsageStatsRepositoryImpl): DefaultLifecycleObserver

    @Binds
    @Singleton
    fun bindStorageStatsRepository(impl: StorageStatsRepositoryImpl): StorageStatsRepository

    @Binds
    @IntoSet
    @Singleton
    fun bindStorageStatsAsLifecycleObserver(impl: StorageStatsRepositoryImpl): DefaultLifecycleObserver

    companion object {
        @Provides
        @Singleton
        fun provideUsageStatsManager(@ApplicationContext context: Context): UsageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        @Provides
        @Singleton
        fun provideStorageStatsManager(@ApplicationContext context: Context): StorageStatsManager = context.getSystemService(StorageStatsManager::class.java)

        @Provides
        @Singleton
        fun provideAppOpsManager(@ApplicationContext context: Context): AppOpsManager = context.getSystemService(AppOpsManager::class.java)
    }
}
