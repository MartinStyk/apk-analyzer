package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.source

import androidx.compose.runtime.Immutable
import sk.styk.martin.apkanalyzer.core.common.model.AppSource

@Immutable
data class SourceFilterState(val options: List<SourceOption> = listOf())

@Immutable
data class SourceOption(val source: AppSource, val isSelected: Boolean)
