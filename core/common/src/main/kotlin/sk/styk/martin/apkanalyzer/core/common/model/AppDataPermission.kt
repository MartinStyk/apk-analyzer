package sk.styk.martin.apkanalyzer.core.common.model

sealed interface AppDataPermission {
    data object UsageAccess : AppDataPermission
    data object StorageAccess : AppDataPermission
}
