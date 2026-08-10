package sk.styk.martin.apkanalyzer.core.apps.di

import android.app.AppOpsManager
import android.app.Application
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.DefaultLifecycleObserver
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.AppExportManager
import sk.styk.martin.apkanalyzer.core.apps.AppExportManagerImpl
import sk.styk.martin.apkanalyzer.core.apps.AppSigningRepository
import sk.styk.martin.apkanalyzer.core.apps.AppSigningRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.DeviceFeaturesRepository
import sk.styk.martin.apkanalyzer.core.apps.DeviceFeaturesRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.PackageChangesObserver
import sk.styk.martin.apkanalyzer.core.apps.PackageChangesObserverImpl
import sk.styk.martin.apkanalyzer.core.apps.StorageStatsRepository
import sk.styk.martin.apkanalyzer.core.apps.StorageStatsRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.UsageStatsRepository
import sk.styk.martin.apkanalyzer.core.apps.UsageStatsRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.analysis.ApkSigningBlockAnalyzer
import sk.styk.martin.apkanalyzer.core.apps.analysis.ApkSigningBlockAnalyzerImpl
import sk.styk.martin.apkanalyzer.core.apps.analysis.ApkSigningBlockParser
import sk.styk.martin.apkanalyzer.core.apps.analysis.ApkSigningBlockParserImpl
import sk.styk.martin.apkanalyzer.core.apps.analysis.CertificateExtractor
import sk.styk.martin.apkanalyzer.core.apps.analysis.CertificateExtractorImpl
import sk.styk.martin.apkanalyzer.core.apps.analysis.ComponentManifestParser
import sk.styk.martin.apkanalyzer.core.apps.analysis.ComponentManifestParserImpl
import sk.styk.martin.apkanalyzer.core.apps.analysis.InstallSourceResolver
import sk.styk.martin.apkanalyzer.core.apps.analysis.InstallSourceResolverImpl
import sk.styk.martin.apkanalyzer.core.apps.analysis.ManifestParser
import sk.styk.martin.apkanalyzer.core.apps.analysis.ManifestParserImpl
import sk.styk.martin.apkanalyzer.core.apps.analysis.ManifestXmlRenderer
import sk.styk.martin.apkanalyzer.core.apps.analysis.ManifestXmlRendererImpl
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal interface AppsModule {
    @Binds
    @Singleton
    fun bindInstalledAppsRepository(impl: InstalledAppsRepositoryImpl): InstalledAppsRepository

    @Binds
    @Singleton
    fun bindPackageChangesObserver(impl: PackageChangesObserverImpl): PackageChangesObserver

    @Binds
    @Singleton
    fun bindAppDetailRepository(impl: AppDetailRepositoryImpl): AppDetailRepository

    @Binds
    @Singleton
    fun bindDeviceFeaturesRepository(impl: DeviceFeaturesRepositoryImpl): DeviceFeaturesRepository

    @Binds
    @Singleton
    fun bindAppSigningRepository(impl: AppSigningRepositoryImpl): AppSigningRepository

    @Binds
    fun bindInstallSourceResolver(impl: InstallSourceResolverImpl): InstallSourceResolver

    @Binds
    fun bindCertificateExtractor(impl: CertificateExtractorImpl): CertificateExtractor

    @Binds
    @Singleton
    fun bindApkSigningBlockAnalyzer(impl: ApkSigningBlockAnalyzerImpl): ApkSigningBlockAnalyzer

    @Binds
    @Singleton
    fun bindApkSigningBlockParser(impl: ApkSigningBlockParserImpl): ApkSigningBlockParser

    @Binds
    fun bindManifestParser(impl: ManifestParserImpl): ManifestParser

    @Binds
    fun bindComponentManifestParser(impl: ComponentManifestParserImpl): ComponentManifestParser

    @Binds
    fun bindManifestXmlRenderer(impl: ManifestXmlRendererImpl): ManifestXmlRenderer

    @Binds
    @Singleton
    fun bindAppExportManager(impl: AppExportManagerImpl): AppExportManager

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
        fun providePackageManager(application: Application): PackageManager = application.packageManager

        @Provides
        @Singleton
        fun provideUsageStatsManager(@ApplicationContext context: Context): UsageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        @Provides
        @Singleton
        fun provideStorageStatsManager(@ApplicationContext context: Context): StorageStatsManager = context.getSystemService(StorageStatsManager::class.java)

        @Provides
        @Singleton
        fun provideAppOpsManager(@ApplicationContext context: Context): AppOpsManager = context.getSystemService(AppOpsManager::class.java)
    }
}
