package sk.styk.martin.apkanalyzer.core.common.settings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PersistenceModule {
    @Binds
    @Singleton
    abstract fun bindPersistenceRepository(impl: DataStorePersistenceRepository): PersistenceRepository
}
