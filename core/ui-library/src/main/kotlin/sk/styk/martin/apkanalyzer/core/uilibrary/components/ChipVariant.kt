package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.runtime.Immutable

@Immutable
sealed interface ChipVariant {
    data object Default : ChipVariant
    data object Tonal : ChipVariant
    data object Positive : ChipVariant
    data object Warning : ChipVariant
    data object Negative : ChipVariant
}
