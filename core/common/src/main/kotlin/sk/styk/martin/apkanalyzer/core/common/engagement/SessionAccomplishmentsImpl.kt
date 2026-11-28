package sk.styk.martin.apkanalyzer.core.common.engagement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SessionAccomplishmentsImpl @Inject constructor() : SessionAccomplishments {

    final override val hasAccomplishment: StateFlow<Boolean>
        field = MutableStateFlow(false)

    override fun record() {
        hasAccomplishment.value = true
    }
}
