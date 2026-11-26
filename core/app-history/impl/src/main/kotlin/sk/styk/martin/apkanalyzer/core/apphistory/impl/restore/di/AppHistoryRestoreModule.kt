package sk.styk.martin.apkanalyzer.core.apphistory.impl.restore.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.apphistory.impl.restore.AppHistoryRestoreMerger
import sk.styk.martin.apkanalyzer.core.apphistory.impl.restore.AppHistoryRestoreMergerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppHistoryRestoreModule {
    @Binds
    @Singleton
    fun bindAppHistoryRestoreMerger(impl: AppHistoryRestoreMergerImpl): AppHistoryRestoreMerger
}
