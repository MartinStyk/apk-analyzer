package sk.styk.martin.apkanalyzer.core.appexport.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.appexport.InventoryExportManager
import sk.styk.martin.apkanalyzer.core.appexport.InventoryExportManagerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppExportModule {
    @Binds
    @Singleton
    fun bindInventoryExportManager(impl: InventoryExportManagerImpl): InventoryExportManager
}
