package sk.styk.martin.apkanalyzer.core.common.engagement

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface EngagementModule {

    @Binds
    @Singleton
    fun bindSessionAccomplishments(impl: SessionAccomplishmentsImpl): SessionAccomplishments
}
