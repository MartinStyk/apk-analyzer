package sk.styk.martin.apkanalyzer.core.appfunctions.usecase

import android.content.pm.PackageManager
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionException

internal fun notInstalled(packageName: String): AppFunctionElementNotFoundException =
    AppFunctionElementNotFoundException("No installed app with package name $packageName")

internal fun appDetailLookupFailure(packageName: String, cause: Throwable): AppFunctionException = when (cause) {
    is PackageManager.NameNotFoundException -> notInstalled(packageName)
    else -> AppFunctionAppUnknownException("Failed to analyze $packageName: ${cause.message}")
}
