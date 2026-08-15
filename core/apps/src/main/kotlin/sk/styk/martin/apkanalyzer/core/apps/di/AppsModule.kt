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
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.PackageChangesObserver
import sk.styk.martin.apkanalyzer.core.apps.PackageChangesObserverImpl
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.DeviceFeaturesRepository
import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.DeviceFeaturesRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.export.AppExportManager
import sk.styk.martin.apkanalyzer.core.apps.export.AppExportManagerImpl
import sk.styk.martin.apkanalyzer.core.apps.installsource.InstallSourceResolver
import sk.styk.martin.apkanalyzer.core.apps.installsource.InstallSourceResolverImpl
import sk.styk.martin.apkanalyzer.core.apps.intentfilters.IntentFiltersRepository
import sk.styk.martin.apkanalyzer.core.apps.intentfilters.IntentFiltersRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.manifest.ComponentManifestParser
import sk.styk.martin.apkanalyzer.core.apps.manifest.ComponentManifestParserImpl
import sk.styk.martin.apkanalyzer.core.apps.manifest.ManifestParser
import sk.styk.martin.apkanalyzer.core.apps.manifest.ManifestParserImpl
import sk.styk.martin.apkanalyzer.core.apps.manifest.ManifestXmlRenderer
import sk.styk.martin.apkanalyzer.core.apps.manifest.ManifestXmlRendererImpl
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibrariesRepository
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibrariesRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.permissions.PermissionDefinitionResolver
import sk.styk.martin.apkanalyzer.core.apps.permissions.PermissionDefinitionResolverImpl
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibrariesRepository
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibrariesRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.signing.ApkSigningBlockAnalyzer
import sk.styk.martin.apkanalyzer.core.apps.signing.ApkSigningBlockAnalyzerImpl
import sk.styk.martin.apkanalyzer.core.apps.signing.ApkSigningBlockParser
import sk.styk.martin.apkanalyzer.core.apps.signing.ApkSigningBlockParserImpl
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigningRepository
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigningRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateExtractor
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificateExtractorImpl
import sk.styk.martin.apkanalyzer.core.apps.storagestats.StorageStatsRepository
import sk.styk.martin.apkanalyzer.core.apps.storagestats.StorageStatsRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.usagestats.UsageStatsRepository
import sk.styk.martin.apkanalyzer.core.apps.usagestats.UsageStatsRepositoryImpl
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal interface AppsModule {

    // root
    @Binds
    @Singleton
    fun bindInstalledAppsRepository(impl: InstalledAppsRepositoryImpl): InstalledAppsRepository

    @Binds
    @Singleton
    fun bindPackageChangesObserver(impl: PackageChangesObserverImpl): PackageChangesObserver

    @Binds
    @Singleton
    fun bindAppDetailRepository(impl: AppDetailRepositoryImpl): AppDetailRepository

    // signing
    @Binds
    @Singleton
    fun bindAppSigningRepository(impl: AppSigningRepositoryImpl): AppSigningRepository

    @Binds
    fun bindCertificateExtractor(impl: CertificateExtractorImpl): CertificateExtractor

    @Binds
    @Singleton
    fun bindApkSigningBlockAnalyzer(impl: ApkSigningBlockAnalyzerImpl): ApkSigningBlockAnalyzer

    @Binds
    @Singleton
    fun bindApkSigningBlockParser(impl: ApkSigningBlockParserImpl): ApkSigningBlockParser

    // permissions
    @Binds
    @Singleton
    fun bindPermissionDefinitionResolver(impl: PermissionDefinitionResolverImpl): PermissionDefinitionResolver

    // manifest
    @Binds
    fun bindManifestParser(impl: ManifestParserImpl): ManifestParser

    @Binds
    fun bindComponentManifestParser(impl: ComponentManifestParserImpl): ComponentManifestParser

    @Binds
    fun bindManifestXmlRenderer(impl: ManifestXmlRendererImpl): ManifestXmlRenderer

    // intentfilters
    @Binds
    @Singleton
    fun bindIntentFiltersRepository(impl: IntentFiltersRepositoryImpl): IntentFiltersRepository

    // installsource
    @Binds
    fun bindInstallSourceResolver(impl: InstallSourceResolverImpl): InstallSourceResolver

    // devicefeatures
    @Binds
    @Singleton
    fun bindDeviceFeaturesRepository(impl: DeviceFeaturesRepositoryImpl): DeviceFeaturesRepository

    // packaging
    @Binds
    @Singleton
    fun bindNativeLibrariesRepository(impl: NativeLibrariesRepositoryImpl): NativeLibrariesRepository

    // usagestats
    @Binds
    @Singleton
    fun bindUsageStatsRepository(impl: UsageStatsRepositoryImpl): UsageStatsRepository

    @Binds
    @IntoSet
    @Singleton
    fun bindUsageStatsAsLifecycleObserver(impl: UsageStatsRepositoryImpl): DefaultLifecycleObserver

    // storagestats
    @Binds
    @Singleton
    fun bindStorageStatsRepository(impl: StorageStatsRepositoryImpl): StorageStatsRepository

    @Binds
    @IntoSet
    @Singleton
    fun bindStorageStatsAsLifecycleObserver(impl: StorageStatsRepositoryImpl): DefaultLifecycleObserver

    // export
    @Binds
    @Singleton
    fun bindAppExportManager(impl: AppExportManagerImpl): AppExportManager

    companion object {
        @Provides
        @Singleton
        fun providePackageManager(application: Application): PackageManager = application.packageManager

        @Provides
        @Singleton
        fun provideUsageStatsManager(@ApplicationContext context: Context): UsageStatsManager {
            val service = context.getSystemService(Context.USAGE_STATS_SERVICE)
            return service as UsageStatsManager
        }

        @Provides
        @Singleton
        fun provideStorageStatsManager(@ApplicationContext context: Context): StorageStatsManager = context.getSystemService(StorageStatsManager::class.java)

        @Provides
        @Singleton
        fun provideAppOpsManager(@ApplicationContext context: Context): AppOpsManager = context.getSystemService(AppOpsManager::class.java)
    }
}
