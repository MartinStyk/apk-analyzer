package sk.styk.martin.apkanalyzer.core.aiinsights.impl.di

import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.aiinsights.impl.ai.OnDeviceAiEngine
import sk.styk.martin.apkanalyzer.core.aiinsights.impl.ai.OnDeviceAiEngineImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AiInsightsModule {

    @Binds
    @Singleton
    fun bindOnDeviceAiEngine(impl: OnDeviceAiEngineImpl): OnDeviceAiEngine

    companion object {
        @Provides
        @Singleton
        fun provideGenerativeModel(): GenerativeModel = Generation.getClient()
    }
}
