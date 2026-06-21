package sk.styk.martin.apkanalyzer.core.common.clipboard

import android.content.ClipData
import android.os.Build
import javax.inject.Inject
import android.content.ClipboardManager as AndroidClipboardManager

internal class ClipboardManagerImpl @Inject constructor(private val clipboardManager: AndroidClipboardManager) : ClipboardManager {

    override fun copy(label: String, value: String): CopyResult {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value))
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            CopyResult.FeedbackShown
        } else {
            CopyResult.FeedbackNotShown
        }
    }
}
