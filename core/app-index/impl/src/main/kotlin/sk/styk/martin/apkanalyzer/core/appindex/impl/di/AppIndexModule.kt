package sk.styk.martin.apkanalyzer.core.appindex.impl.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.appindex.api.AppIndexRepository
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseBucketDetailResolver
import sk.styk.martin.apkanalyzer.core.appindex.api.browse.BrowseDimensionLabeler
import sk.styk.martin.apkanalyzer.core.appindex.impl.AppIndexRepositoryImpl
import sk.styk.martin.apkanalyzer.core.appindex.impl.browse.BrowseBucketDetailResolverImpl
import sk.styk.martin.apkanalyzer.core.appindex.impl.browse.BrowseDimensionLabelerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppIndexModule {
    @Binds
    @Singleton
    fun bindAppIndexRepository(impl: AppIndexRepositoryImpl): AppIndexRepository

    @Binds
    fun bindBrowseBucketDetailResolver(impl: BrowseBucketDetailResolverImpl): BrowseBucketDetailResolver

    @Binds
    fun bindBrowseDimensionLabeler(impl: BrowseDimensionLabelerImpl): BrowseDimensionLabeler
}
