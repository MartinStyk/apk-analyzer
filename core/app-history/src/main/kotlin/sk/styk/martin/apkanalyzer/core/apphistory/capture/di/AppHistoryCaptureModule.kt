package sk.styk.martin.apkanalyzer.core.apphistory.capture.di

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import sk.styk.martin.apkanalyzer.core.apphistory.capture.AppHistoryCaptureRepository
import sk.styk.martin.apkanalyzer.core.apphistory.capture.AppHistoryCaptureRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apphistory.capture.AppHistoryCaptureScheduler
import sk.styk.martin.apkanalyzer.core.apphistory.capture.AppHistoryCaptureSchedulerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppHistoryCaptureModule {
    @Binds
    @Singleton
    fun bindAppHistoryCaptureRepository(impl: AppHistoryCaptureRepositoryImpl): AppHistoryCaptureRepository

    @Binds
    @Singleton
    fun bindAppHistoryCaptureScheduler(impl: AppHistoryCaptureSchedulerImpl): AppHistoryCaptureScheduler

    @Binds
    @Singleton
    @IntoSet
    fun bindAppHistoryCaptureSchedulerAsLifecycleObserver(impl: AppHistoryCaptureSchedulerImpl): DefaultLifecycleObserver
}

@Module
@InstallIn(SingletonComponent::class)
internal class AppHistoryWorkModule {
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)
}
