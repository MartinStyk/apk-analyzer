package sk.styk.martin.apkanalyzer.core.common.review

import android.content.Context
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ReviewModule {

    @Binds
    @Singleton
    abstract fun bindReviewEligibilityTracker(impl: ReviewEligibilityTrackerImpl): ReviewEligibilityTracker

    @Binds
    @Singleton
    abstract fun bindInAppReviewLauncher(impl: InAppReviewLauncherImpl): InAppReviewLauncher

    companion object {
        @Provides
        fun provideReviewManager(@ApplicationContext context: Context): ReviewManager = ReviewManagerFactory.create(context)
    }
}
