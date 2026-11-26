package sk.styk.martin.apkanalyzer.core.apps.impl.di

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
import sk.styk.martin.apkanalyzer.core.apps.api.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.api.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.api.PackageChangesObserver
import sk.styk.martin.apkanalyzer.core.apps.api.devicefeatures.DeviceFeaturesRepository
import sk.styk.martin.apkanalyzer.core.apps.api.export.AppExportManager
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.AppFilterRepositories
import sk.styk.martin.apkanalyzer.core.apps.api.filtering.AppFilterRepository
import sk.styk.martin.apkanalyzer.core.apps.api.intentfilters.IntentFiltersRepository
import sk.styk.martin.apkanalyzer.core.apps.api.manifest.ManifestParser
import sk.styk.martin.apkanalyzer.core.apps.api.packaging.NativeLibrariesRepository
import sk.styk.martin.apkanalyzer.core.apps.api.permissions.PermissionDefinitionResolver
import sk.styk.martin.apkanalyzer.core.apps.api.sdkversion.SdkVersionResolver
import sk.styk.martin.apkanalyzer.core.apps.api.signing.AppSigningRepository
import sk.styk.martin.apkanalyzer.core.apps.api.signing.SigningSchemeRepository
import sk.styk.martin.apkanalyzer.core.apps.api.storagestats.StorageStatsRepository
import sk.styk.martin.apkanalyzer.core.apps.api.usagestats.UsageStatsRepository
import sk.styk.martin.apkanalyzer.core.apps.impl.AppDetailRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.InstalledAppsRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.PackageChangesObserverImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.devicefeatures.DeviceFeaturesRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.export.AppExportManagerImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.filtering.AppFilterRepositoriesImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.filtering.AppFilterRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.installsource.InstallSourceResolver
import sk.styk.martin.apkanalyzer.core.apps.impl.installsource.InstallSourceResolverImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.intentfilters.IntentFiltersRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.manifest.ComponentManifestParser
import sk.styk.martin.apkanalyzer.core.apps.impl.manifest.ComponentManifestParserImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.manifest.ManifestParserImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.manifest.ManifestXmlRenderer
import sk.styk.martin.apkanalyzer.core.apps.impl.manifest.ManifestXmlRendererImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.packaging.NativeLibrariesRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.permissions.PermissionDefinitionResolverImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.sdkversion.SdkVersionResolverImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.signing.ApkSigningBlockAnalyzer
import sk.styk.martin.apkanalyzer.core.apps.impl.signing.ApkSigningBlockAnalyzerImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.signing.ApkSigningBlockParser
import sk.styk.martin.apkanalyzer.core.apps.impl.signing.ApkSigningBlockParserImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.signing.AppSigningRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.signing.CertificateExtractor
import sk.styk.martin.apkanalyzer.core.apps.impl.signing.CertificateExtractorImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.signing.SigningSchemeRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.storagestats.StorageStatsRepositoryImpl
import sk.styk.martin.apkanalyzer.core.apps.impl.usagestats.UsageStatsRepositoryImpl
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
    fun bindAppSigningRepository(impl: AppSigningRepositoryImpl): AppSigningRepository

    @Binds
    fun bindCertificateExtractor(impl: CertificateExtractorImpl): CertificateExtractor

    @Binds
    @Singleton
    fun bindApkSigningBlockAnalyzer(impl: ApkSigningBlockAnalyzerImpl): ApkSigningBlockAnalyzer

    @Binds
    @Singleton
    fun bindApkSigningBlockParser(impl: ApkSigningBlockParserImpl): ApkSigningBlockParser

    @Binds
    @Singleton
    fun bindSigningSchemeRepository(impl: SigningSchemeRepositoryImpl): SigningSchemeRepository

    @Binds
    @Singleton
    fun bindPermissionDefinitionResolver(impl: PermissionDefinitionResolverImpl): PermissionDefinitionResolver

    @Binds
    fun bindManifestParser(impl: ManifestParserImpl): ManifestParser

    @Binds
    fun bindComponentManifestParser(impl: ComponentManifestParserImpl): ComponentManifestParser

    @Binds
    fun bindManifestXmlRenderer(impl: ManifestXmlRendererImpl): ManifestXmlRenderer

    @Binds
    @Singleton
    fun bindIntentFiltersRepository(impl: IntentFiltersRepositoryImpl): IntentFiltersRepository

    @Binds
    fun bindInstallSourceResolver(impl: InstallSourceResolverImpl): InstallSourceResolver

    @Binds
    @Singleton
    fun bindDeviceFeaturesRepository(impl: DeviceFeaturesRepositoryImpl): DeviceFeaturesRepository

    @Binds
    @Singleton
    fun bindNativeLibrariesRepository(impl: NativeLibrariesRepositoryImpl): NativeLibrariesRepository

    @Binds
    @Singleton
    fun bindAppFilterRepository(impl: AppFilterRepositoryImpl): AppFilterRepository

    @Binds
    @Singleton
    fun bindAppFilterRepositories(impl: AppFilterRepositoriesImpl): AppFilterRepositories

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

    @Binds
    fun bindSdkVersionResolver(impl: SdkVersionResolverImpl): SdkVersionResolver

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
