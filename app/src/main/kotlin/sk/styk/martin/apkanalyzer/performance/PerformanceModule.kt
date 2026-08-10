package sk.styk.martin.apkanalyzer.performance

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.common.performance.PerformanceTracker
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PerformanceModule {
    @Binds
    @Singleton
    fun bindPerformanceTracker(implementation: FirebasePerformanceTracker): PerformanceTracker
}
