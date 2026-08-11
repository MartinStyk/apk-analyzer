package sk.styk.martin.apkanalyzer.core.common.performance

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PerformanceModule {
    @Binds
    @Singleton
    fun bindPerformanceTracker(implementation: FirebasePerformanceTracker): PerformanceTracker
}
