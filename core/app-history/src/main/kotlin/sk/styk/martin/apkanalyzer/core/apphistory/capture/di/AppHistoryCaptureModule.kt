package sk.styk.martin.apkanalyzer.core.apphistory.capture.di

import androidx.lifecycle.DefaultLifecycleObserver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
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
