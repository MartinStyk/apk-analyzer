package sk.styk.martin.apkanalyzer.core.common.clipboard

sealed interface CopyResult {
    data object FeedbackShown : CopyResult
    data object FeedbackNotShown : CopyResult
}

interface ClipboardManager {
    fun copy(label: String, value: String): CopyResult
}
