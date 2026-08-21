package sk.styk.martin.apkanalyzer.core.common.applanguage

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppLanguageModule {
    @Binds
    @Singleton
    fun bindAppLanguageRepository(impl: AppLanguageRepositoryImpl): AppLanguageRepository
}
