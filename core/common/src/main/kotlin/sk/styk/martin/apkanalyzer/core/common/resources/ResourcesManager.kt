package sk.styk.martin.apkanalyzer.core.common.resources

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ArrayRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.Dimension
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ResourcesManager
@Inject
constructor(@ApplicationContext val context: Context) {
    fun getString(@StringRes stringRes: Int, vararg args: Any): CharSequence = context.getString(stringRes, *args)

    fun getStringArray(@ArrayRes stringArrayRes: Int): Array<String> = context.resources.getStringArray(stringArrayRes)

    @ColorInt
    fun getColor(@ColorRes colorRes: Int): Int = ResourcesCompat.getColor(context.resources, colorRes, context.theme)

    @Dimension(unit = Dimension.DP)
    fun getDisplayHeight(): Float = context.resources.displayMetrics.run { heightPixels / density }

    fun luminance(@ColorInt foreground: Int) = ColorUtils.calculateLuminance(foreground)
}
