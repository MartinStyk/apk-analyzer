package sk.styk.martin.apkanalyzer.core.apkfiles.impl.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.apkfiles.api.TemporaryApkManager
import sk.styk.martin.apkanalyzer.core.apkfiles.impl.TemporaryApkManagerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TemporaryApkModule {

    @Binds
    @Singleton
    fun bindTemporaryApkManager(impl: TemporaryApkManagerImpl): TemporaryApkManager
}
