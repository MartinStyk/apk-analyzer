package sk.styk.martin.apkanalyzer.core.common.file

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface FileModule {

    @Binds
    @Singleton
    fun bindShareFileManager(impl: ShareFileManagerImpl): ShareFileManager

    @Binds
    @Singleton
    fun bindDocumentFileManager(impl: DocumentFileManagerImpl): DocumentFileManager
}
