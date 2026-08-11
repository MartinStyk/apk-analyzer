package sk.styk.martin.apkanalyzer.core.appaidescription.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiDescriptionRepository
import sk.styk.martin.apkanalyzer.core.appaidescription.AppAiDescriptionRepositoryImpl
import sk.styk.martin.apkanalyzer.core.appaidescription.cache.AppAiDescriptionCache
import sk.styk.martin.apkanalyzer.core.appaidescription.cache.AppAiDescriptionCacheImpl
import sk.styk.martin.apkanalyzer.core.appaidescription.cache.AppAiDescriptionDao
import sk.styk.martin.apkanalyzer.core.appaidescription.cache.AppAiDescriptionDatabase
import sk.styk.martin.apkanalyzer.core.appaidescription.generation.AiDescriptionGenerator
import sk.styk.martin.apkanalyzer.core.appaidescription.generation.MlKitAiDescriptionGenerator
import sk.styk.martin.apkanalyzer.core.appaidescription.metadata.AppDetailMetadataProviderImpl
import sk.styk.martin.apkanalyzer.core.appaidescription.metadata.AppMetadataProvider
import javax.inject.Singleton

private const val DATABASE_NAME = "app_ai_description.db"

@Module
@InstallIn(SingletonComponent::class)
internal interface AppAiDescriptionModule {

    @Binds
    @Singleton
    fun bindAppAiDescriptionRepository(impl: AppAiDescriptionRepositoryImpl): AppAiDescriptionRepository

    @Binds
    @Singleton
    fun bindAppMetadataProvider(impl: AppDetailMetadataProviderImpl): AppMetadataProvider

    @Binds
    @Singleton
    fun bindAiDescriptionGenerator(impl: MlKitAiDescriptionGenerator): AiDescriptionGenerator

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
