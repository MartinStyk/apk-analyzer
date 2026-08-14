package sk.styk.martin.apkanalyzer.feature.apps.impl.filter.source

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.common.model.AppSource

@Immutable
data class SourceFilterState(val options: ImmutableList<SourceOption> = persistentListOf())

@Immutable
data class SourceOption(val source: AppSource, val isSelected: Boolean)
