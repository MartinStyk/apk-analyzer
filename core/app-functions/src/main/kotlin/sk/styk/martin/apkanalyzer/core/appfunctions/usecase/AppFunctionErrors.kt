package sk.styk.martin.apkanalyzer.core.appfunctions.usecase

import androidx.appfunctions.AppFunctionElementNotFoundException

internal fun notInstalled(packageName: String): AppFunctionElementNotFoundException =
    AppFunctionElementNotFoundException("No installed app with package name $packageName")
