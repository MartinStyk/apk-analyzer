package sk.styk.martin.apkanalyzer.core.appexport.impl.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.appexport.api.InventoryExportManager
import sk.styk.martin.apkanalyzer.core.appexport.api.preferences.InventoryExportPreferences
import sk.styk.martin.apkanalyzer.core.appexport.impl.InventoryExportManagerImpl
import sk.styk.martin.apkanalyzer.core.appexport.impl.preferences.InventoryExportPreferencesImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppExportModule {
    @Binds
    @Singleton
    fun bindInventoryExportManager(impl: InventoryExportManagerImpl): InventoryExportManager

    @Binds
    @Singleton
    fun bindInventoryExportPreferences(impl: InventoryExportPreferencesImpl): InventoryExportPreferences
}
