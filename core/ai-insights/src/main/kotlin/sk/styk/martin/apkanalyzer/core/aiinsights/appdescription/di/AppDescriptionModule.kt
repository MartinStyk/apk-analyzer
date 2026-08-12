package sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.AppAiDescriptionRepository
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.AppAiDescriptionRepositoryImpl
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.cache.AppAiDescriptionCache
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.cache.AppAiDescriptionCacheImpl
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.cache.AppAiDescriptionDao
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.cache.AppAiDescriptionDatabase
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.generation.AiDescriptionGenerator
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.generation.AiDescriptionGeneratorImpl
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.metadata.AppDetailMetadataProviderImpl
import sk.styk.martin.apkanalyzer.core.aiinsights.appdescription.metadata.AppMetadataProvider
import javax.inject.Singleton

private const val DATABASE_NAME = "app_ai_description.db"

@Module
@InstallIn(SingletonComponent::class)
internal interface AppDescriptionModule {

    @Binds
    @Singleton
    fun bindAppAiDescriptionRepository(impl: AppAiDescriptionRepositoryImpl): AppAiDescriptionRepository

    @Binds
    @Singleton
    fun bindAppMetadataProvider(impl: AppDetailMetadataProviderImpl): AppMetadataProvider

    @Binds
    @Singleton
    fun bindAiDescriptionGenerator(impl: AiDescriptionGeneratorImpl): AiDescriptionGenerator

    @Binds
    @Singleton
    fun bindAppAiDescriptionCache(impl: AppAiDescriptionCacheImpl): AppAiDescriptionCache

    companion object {
        @Provides
        @Singleton
        fun provideAppAiDescriptionDatabase(@ApplicationContext context: Context): AppAiDescriptionDatabase =
            Room.databaseBuilder(context, AppAiDescriptionDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        @Provides
        @Singleton
        fun provideAppAiDescriptionDao(database: AppAiDescriptionDatabase): AppAiDescriptionDao = database.appAiDescriptionDao()
    }
}
