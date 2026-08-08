package sk.styk.martin.apkanalyzer.core.common.settings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface PersistenceModule {
    @Binds
    @Singleton
    fun bindPersistenceRepository(impl: DataStorePersistenceRepository): PersistenceRepository
}
