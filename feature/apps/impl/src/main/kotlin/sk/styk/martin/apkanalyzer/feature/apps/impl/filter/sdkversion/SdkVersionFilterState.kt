package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.sdkversion

import androidx.compose.runtime.Immutable

@Immutable
data class SdkVersionFilterState(val options: List<SdkVersionOption> = listOf())

@Immutable
data class SdkVersionOption(
    val sdkVersion: Int,
    val isSelected: Boolean,
    val androidVersionName: String?,
)
