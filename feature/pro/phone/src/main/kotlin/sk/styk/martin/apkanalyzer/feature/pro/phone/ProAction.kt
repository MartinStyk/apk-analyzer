package sk.styk.martin.apkanalyzer.feature.pro.phone

import android.app.Activity

internal sealed interface ProAction {
    data class Unlock(val activity: Activity) : ProAction

    data object Restore : ProAction
}
