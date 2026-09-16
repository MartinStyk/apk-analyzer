package sk.styk.martin.apkanalyzer.core.apphistory.storage.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryDatabase
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryGateDao
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryReadDao
import sk.styk.martin.apkanalyzer.core.apphistory.storage.AppHistoryWriteDao
import javax.inject.Singleton

private const val DATABASE_NAME = "app_history.db"

@Module
@InstallIn(SingletonComponent::class)
internal object AppHistoryStorageModule {
    @Provides
    @Singleton
    fun provideAppHistoryDatabase(@ApplicationContext context: Context): AppHistoryDatabase =
        Room.databaseBuilder(context, AppHistoryDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun provideAppHistoryGateDao(database: AppHistoryDatabase): AppHistoryGateDao = database.appHistoryGateDao()

    @Provides
    fun provideAppHistoryWriteDao(database: AppHistoryDatabase): AppHistoryWriteDao = database.appHistoryWriteDao()

    @Provides
    fun provideAppHistoryReadDao(database: AppHistoryDatabase): AppHistoryReadDao = database.appHistoryReadDao()
}
