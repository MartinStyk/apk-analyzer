package sk.styk.martin.apkanalyzer.core.appanalysis.di

import android.app.Application
import android.content.pm.PackageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class AppAnalysisModule {
    @Provides
    @Singleton
    fun providePackageManager(application: Application): PackageManager = application.packageManager
}
