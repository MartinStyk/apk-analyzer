package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import androidx.compose.material3.RangeSlider as MaterialRangeSlider

@Composable
fun RangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    MaterialRangeSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        colors = SliderDefaults.colors(
            thumbColor = AppTheme.colors.primary,
            activeTrackColor = AppTheme.colors.primary,
            inactiveTrackColor = AppTheme.colors.surfaceVariant,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun RangeSliderPreview() {
    ApkAnalyzerTheme {
        RangeSlider(value = 21f..35f, onValueChange = {}, valueRange = 1f..36f)
    }
}
