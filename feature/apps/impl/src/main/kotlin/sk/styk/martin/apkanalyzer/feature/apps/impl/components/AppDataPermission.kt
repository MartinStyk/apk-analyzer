package sk.styk.martin.apkanalyzer.feature.apps.impl.components

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AppDataPermission {
    data object UsageAccess : AppDataPermission
    data object StorageAccess : AppDataPermission
}
