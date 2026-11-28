package sk.styk.martin.apkanalyzer.core.common.engagement

import kotlinx.coroutines.flow.StateFlow

interface SessionAccomplishments {
    val hasAccomplishment: StateFlow<Boolean>

    fun record()
}
