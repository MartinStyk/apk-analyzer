package sk.styk.martin.apkanalyzer.core.common.review

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val QUALIFIED_SESSION_THRESHOLD = 2

@Singleton
internal class ReviewEligibilityTrackerImpl @Inject constructor(
    private val persistenceRepository: PersistenceRepository,
    private val applicationScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
) : ReviewEligibilityTracker {

    override val reviewPromptRequests: Flow<Unit> = combine(
        persistenceRepository.observe(Key.AppDetailQualifiedSessionCount),
        persistenceRepository.observe(Key.ReviewFlowRequested),
    ) { qualifiedSessionCount, alreadyRequested ->
        !alreadyRequested && qualifiedSessionCount >= QUALIFIED_SESSION_THRESHOLD
    }
        .distinctUntilChanged()
        .filter { eligible -> eligible }
        .map { }

    override fun recordAppDetailSessionCompleted(qualified: Boolean) {
        if (!qualified) return
        applicationScope.launch(dispatcherProvider.io()) {
            val current = persistenceRepository.get(Key.AppDetailQualifiedSessionCount)
            persistenceRepository.save(Key.AppDetailQualifiedSessionCount, current + 1)
        }
    }

    override fun recordPromptShown() {
        applicationScope.launch(dispatcherProvider.io()) {
            persistenceRepository.save(Key.ReviewFlowRequested, true)
        }
    }
}
