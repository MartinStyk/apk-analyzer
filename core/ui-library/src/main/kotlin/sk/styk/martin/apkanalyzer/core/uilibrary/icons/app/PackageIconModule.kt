package sk.styk.martin.apkanalyzer.core.uilibrary.icons.app

import android.content.Context
import android.content.pm.PackageManager
import coil3.ImageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class PackageIconModule {

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context, packageManager: PackageManager): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(PackageIconKeyer())
            add(PackageIconFetcher.Factory(packageManager))
        }
        .build()
}
