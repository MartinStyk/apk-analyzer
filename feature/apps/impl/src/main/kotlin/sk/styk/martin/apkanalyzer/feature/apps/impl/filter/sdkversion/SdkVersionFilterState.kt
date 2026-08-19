package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.sdkversion

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class SdkVersionFilterState(val options: ImmutableList<SdkVersionOption> = persistentListOf())

@Immutable
data class SdkVersionOption(
    val sdkVersion: Int,
    val isSelected: Boolean,
    val androidVersionName: String?,
)
