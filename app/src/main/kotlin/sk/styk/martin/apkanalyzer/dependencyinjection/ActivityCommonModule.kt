package sk.styk.martin.apkanalyzer.dependencyinjection

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@InstallIn(ActivityComponent::class)
@Module
class ActivityCommonModule {
    @Provides
    @ActivityScoped
    fun provideActivityContext(activity: Activity): Context = activity.baseContext

    @Provides
    @ActivityScoped
    fun providesResources(activity: Activity): ComponentActivity = activity as ComponentActivity
}
