package sk.styk.martin.apkanalyzer.core.common.coroutines

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DispatcherModule {
    @Binds
    @Singleton
    fun bindDispatcherProvider(impl: DispatcherProviderImpl): DispatcherProvider
}
