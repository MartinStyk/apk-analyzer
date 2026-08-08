package sk.styk.martin.apkanalyzer.feature.appdetail.impl

import android.content.Context
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

internal fun AppDetailFeedback.message(context: Context): String = when (this) {
    is AppDetailFeedback.ApkSaved -> context.getString(R.string.app_detail_apk_saved, displayName)
    is AppDetailFeedback.IconSaved -> context.getString(R.string.app_detail_icon_saved, displayName)
    AppDetailFeedback.ApkSaveFailed -> context.getString(R.string.app_detail_apk_save_failed)
    AppDetailFeedback.IconSaveFailed -> context.getString(R.string.app_detail_icon_save_failed)
    AppDetailFeedback.DocumentPickerUnavailable -> context.getString(R.string.app_detail_document_picker_unavailable)
}

internal fun formatTimestamp(instant: Instant): String {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return dateFormat.format(Date.from(instant))
}
