package sk.styk.martin.apkanalyzer.core.applist.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.applist.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.applist.InstalledAppsRepositoryImpl
import sk.styk.martin.apkanalyzer.core.applist.PackageChangesObserver
import sk.styk.martin.apkanalyzer.core.applist.PackageChangesObserverImpl
import sk.styk.martin.apkanalyzer.core.applist.RecentlyViewedAppsRepository
import sk.styk.martin.apkanalyzer.core.applist.RecentlyViewedAppsRepositoryImpl
import sk.styk.martin.apkanalyzer.core.applist.SearchHistoryRepository
import sk.styk.martin.apkanalyzer.core.applist.SearchHistoryRepositoryImpl
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
}
