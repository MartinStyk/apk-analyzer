package sk.styk.martin.apkanalyzer.feature.appdetail.impl.components

import androidx.compose.runtime.Immutable

@Immutable
internal data class InfoRow(
    val label: String,
    val value: String,
    val rationale: String,
)
