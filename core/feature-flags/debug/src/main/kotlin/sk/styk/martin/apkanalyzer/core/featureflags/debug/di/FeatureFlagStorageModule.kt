package sk.styk.martin.apkanalyzer.core.featureflags.debug.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.featureFlagDataStore by preferencesDataStore(name = "debug_feature_flags")

@Module
@InstallIn(SingletonComponent::class)
internal class FeatureFlagStorageModule {
    @Provides
    @Singleton
    fun provideFeatureFlagDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.featureFlagDataStore
}
