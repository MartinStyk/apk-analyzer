package sk.styk.martin.apkanalyzer.dependencyinjection

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import sk.styk.martin.apkanalyzer.manager.permission.PermissionManager
import sk.styk.martin.apkanalyzer.manager.permission.PermissionsManagerImpl

@InstallIn(ActivityComponent::class)
@Module
class ActivityCommonModule {
    @Provides
    @ActivityScoped
    fun provideActivityContext(activity: Activity): Context = activity.baseContext

    @Provides
    @ActivityScoped
    fun providesResources(activity: Activity): ComponentActivity = activity as ComponentActivity

    @Provides
    @ActivityScoped
    fun providePermissionsManager(activity: AppCompatActivity): PermissionManager {
        val permissionsManager: PermissionsManagerImpl =
            ViewModelProvider(
                activity,
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return PermissionsManagerImpl(activity.application) as T
                    }
                },
            )[PermissionsManagerImpl::class.java]
        permissionsManager.bind(activity)
        return permissionsManager
    }
}
