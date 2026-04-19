package sk.styk.martin.apkanalyzer.manager.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.textservice.TextInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ClipBoardManager
@Inject
constructor(@field:ApplicationContext private val context: Context) {
    fun copyToClipBoard(text: TextInfo, label: TextInfo = text) {
        copyToClipBoard(text.text, label.text)
    }

    fun copyToClipBoard(text: CharSequence, label: CharSequence = text) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
