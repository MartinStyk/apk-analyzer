package sk.styk.martin.apkanalyzer.feature.pro.phone.support

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SupportPromptSession @Inject constructor() {

    private val paywallShown = AtomicBoolean(false)

    val paywallShownThisSession: Boolean
        get() = paywallShown.get()

    fun recordPaywallShown() {
        paywallShown.set(true)
    }
}
