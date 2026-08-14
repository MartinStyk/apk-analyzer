package sk.styk.martin.apkanalyzer.core.userpreferences.db.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.userpreferences.db.UserPreferencesDatabase
import javax.inject.Singleton

private const val DATABASE_NAME = "user_preferences.db"

@Module
@InstallIn(SingletonComponent::class)
internal object UserPreferencesDatabaseModule {
    @Provides
    @Singleton
    fun provideUserPreferencesDatabase(@ApplicationContext context: Context): UserPreferencesDatabase {
        return Room.databaseBuilder(context, UserPreferencesDatabase::class.java, DATABASE_NAME).build()
    }
}
