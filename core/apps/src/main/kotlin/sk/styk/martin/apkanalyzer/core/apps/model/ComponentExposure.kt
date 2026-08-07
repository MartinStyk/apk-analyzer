package sk.styk.martin.apkanalyzer.core.apps.model

val Activity.isExternallyReachableWithoutPermission: Boolean
    get() = isExported && isLauncher != true && permission.isNullOrBlank()

val Service.isExternallyReachableWithoutPermission: Boolean
    get() = isExported && permission.isNullOrBlank()

val BroadcastReceiver.isExternallyReachableWithoutPermission: Boolean
    get() = isExported && permission.isNullOrBlank()

val ContentProvider.isExternallyReachableWithoutPermission: Boolean
    get() = isExported && (readPermission.isNullOrBlank() || writePermission.isNullOrBlank())
