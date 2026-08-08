package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import sk.styk.martin.apkanalyzer.core.uilibrary.R
import sk.styk.martin.apkanalyzer.core.uilibrary.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

@Suppress("LongParameterList")
@Composable
fun <T> SelectorChip(
    sheetTitle: String,
    options: ImmutableList<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelectOption: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }

    OutlinedChip(
        label = label(selected),
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
                    SelectorOptionRow(
                        label = label(option),
                        selected = option == selected,
                        onClick = {
                            onSelectOption(option)
                            showSheet = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectorOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = AppTheme.typography.bodyLarge,
            color = if (selected) AppTheme.colors.primary else AppTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = ApkAnalyzerIcons.Check,
                tint = AppTheme.colors.primary,
                contentDescription = stringResource(R.string.content_description_selected),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview
@Composable
private fun SelectorChipDefaultPreview() {
    ApkAnalyzerTheme {
        SelectorChip(
            sheetTitle = "Show",
            options = persistentListOf("Requested", "Defined by this app"),
            selected = "Requested",
            label = { it },
            onSelectOption = {},
        )
    }
}

@Preview
@Composable
private fun SelectorChipDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        SelectorChip(
            sheetTitle = "Show",
            options = persistentListOf("Requested", "Defined by this app"),
            selected = "Defined by this app",
            label = { it },
            onSelectOption = {},
        )
    }
}
