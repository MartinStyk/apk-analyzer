package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

@Suppress("LongParameterList")
@Composable
fun <T> MultiSelectorChip(
    sheetTitle: String,
    defaultLabel: String,
    options: ImmutableList<T>,
    selected: ImmutableSet<T>,
    optionLabel: @Composable (T) -> String,
    selectionLabel: @Composable (ImmutableList<T>) -> String,
    onToggleOption: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }
    val selectedOptions = options.filter { it in selected }.toImmutableList()

    OutlinedChip(
        label = if (selectedOptions.isEmpty()) defaultLabel else selectionLabel(selectedOptions),
        selected = selectedOptions.isNotEmpty(),
        onClick = { showSheet = true },
        trailingIcon = ApkAnalyzerIcons.ArrowDropDown,
        modifier = modifier,
    )

    if (showSheet) {
        BottomSheet(onDismiss = { showSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = sheetTitle,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                options.forEach { option ->
                    MultiSelectorOptionRow(
                        label = optionLabel(option),
                        selected = option in selected,
                        onToggle = { onToggleOption(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiSelectorOptionRow(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 2.dp),
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onSurface,
        )
    }
}

@Preview
@Composable
private fun MultiSelectorChipDefaultPreview() {
    ApkAnalyzerTheme {
        MultiSelectorChip(
            sheetTitle = "Protection level",
            defaultLabel = "Protection level",
            options = persistentListOf("Dangerous", "Signature", "Normal"),
            selected = persistentSetOf(),
            optionLabel = { it },
            selectionLabel = { it.first() },
            onToggleOption = {},
        )
    }
}

@Preview
@Composable
private fun MultiSelectorChipSelectedDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        MultiSelectorChip(
            sheetTitle = "Protection level",
            defaultLabel = "Protection level",
            options = persistentListOf("Dangerous", "Signature", "Normal"),
            selected = persistentSetOf("Dangerous", "Signature"),
            optionLabel = { it },
            selectionLabel = { "${it.first()} +${it.size - 1}" },
            onToggleOption = {},
        )
    }
}
