package sk.styk.martin.apkanalyzer.core.common.analytics

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AnalyticsModule {

    @Binds
    @Singleton
    fun bindAnalyticsTracker(implementation: FirebaseAnalyticsTracker): AnalyticsTracker

    companion object {
        @SuppressLint("MissingPermission")
        @Provides
        @Singleton
        fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }
}
