package sk.styk.martin.apkanalyzer.feature.appdetail.domain.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSectionRepository
import sk.styk.martin.apkanalyzer.feature.appdetail.domain.AppDetailSectionRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppDetailDomainModule {
    @Binds
    @Singleton
    fun bindAppDetailSectionRepository(impl: AppDetailSectionRepositoryImpl): AppDetailSectionRepository
}
