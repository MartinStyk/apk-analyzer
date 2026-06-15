package sk.styk.martin.apkanalyzer.core.apps.model

data class BroadcastReceiver(val name: String, val permission: String? = null, val isExported: Boolean = false)
