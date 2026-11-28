package sk.styk.martin.apkanalyzer.feature.pro.phone.support

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.engagement.SessionAccomplishments
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.settings.Key
import sk.styk.martin.apkanalyzer.core.common.settings.PersistenceRepository
import sk.styk.martin.apkanalyzer.core.entitlements.api.Entitlement
import sk.styk.martin.apkanalyzer.core.entitlements.api.EntitlementsRepository
import java.time.Instant
import javax.inject.Inject

private const val TAG = "SupportPrompt"
private const val OFFER_TIMEOUT_MS = 10_000L

class SupportPromptScheduler @Inject internal constructor(
    sessionAccomplishments: SessionAccomplishments,
    private val entitlementsRepository: EntitlementsRepository,
    private val persistenceRepository: PersistenceRepository,
    private val supportPromptSession: SupportPromptSession,
    appInstallInfo: AppInstallInfo,
    private val policy: SupportPromptPolicy,
    dispatcherProvider: DispatcherProvider,
) {

    val promptRequests: Flow<Unit> = flow {
        sessionAccomplishments.hasAccomplishment.first { it }
        val details = withContext(dispatcherProvider.io()) { appInstallInfo.read() }
        val isDue = isDue(details)
        Logger.i(TAG, "Support prompt due: $isDue")
        if (isDue) {
            persistenceRepository.save(Key.SupportPromptShownCount, persistenceRepository[Key.SupportPromptShownCount] + 1)
            persistenceRepository.save(Key.SupportPromptLastVersion, details.versionCode)
            emit(Unit)
        }
    }

    private suspend fun isDue(details: InstallDetails): Boolean {
        entitlementsRepository.refresh()
        val hasPro = Entitlement.Pro in entitlementsRepository.entitlements().first()
        val offers = withTimeoutOrNull(OFFER_TIMEOUT_MS) { entitlementsRepository.offers().first { it.isNotEmpty() } }
        val reviewRequestedAt = persistenceRepository[Key.ReviewFlowRequestedAt].takeIf { it > 0 }?.let(Instant::ofEpochMilli)
        return policy.shouldShow(
            SupportPromptInput(
                now = Instant.now(),
                installedAt = details.installedAt,
                versionCode = details.versionCode,
                lastShownVersionCode = persistenceRepository[Key.SupportPromptLastVersion],
                timesShown = persistenceRepository[Key.SupportPromptShownCount],
                hasAccomplishment = true,
                hasPro = hasPro,
                isPriceAvailable = offers?.any { it.entitlement == Entitlement.Pro } == true,
                paywallShownThisSession = supportPromptSession.paywallShownThisSession,
                reviewRequestedAt = reviewRequestedAt,
            ),
        )
    }
}
