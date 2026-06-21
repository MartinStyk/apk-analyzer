package sk.styk.martin.apkanalyzer.core.common.clipboard

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.ClipboardManager as AndroidClipboardManager

@Module
@InstallIn(SingletonComponent::class)
abstract class ClipboardModule {

    @Binds
    @Singleton
    internal abstract fun bindClipboardManager(impl: ClipboardManagerImpl): ClipboardManager

    companion object {
        @Provides
        fun provideAndroidClipboardManager(@ApplicationContext context: Context): AndroidClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as AndroidClipboardManager
    }
}
