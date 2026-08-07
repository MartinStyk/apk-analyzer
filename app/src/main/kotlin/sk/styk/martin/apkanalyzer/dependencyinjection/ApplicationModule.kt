package sk.styk.martin.apkanalyzer.dependencyinjection

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class ApplicationModule {
    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    @Provides
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics = FirebaseAnalytics.getInstance(context)
}
