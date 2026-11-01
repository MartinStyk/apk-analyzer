package sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Button
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Toolbar
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.inventoryexport.phone.R

internal val StepContentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)

@Composable
internal fun InventoryExportScaffold(
    currentStep: InventoryExportStep,
    onBack: () -> Unit,
    onSelectStep: (InventoryExportStep) -> Unit,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    canSelectStep: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = stringResource(R.string.inventory_export_title),
            onBack = onBack,
        )
        InventoryStepIndicator(
            currentStep = currentStep,
            canSelectStep = canSelectStep,
            onSelectStep = onSelectStep,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            content()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
        ) {
            bottomBar()
        }
    }
}

@Composable
private fun InventoryStepIndicator(
    currentStep: InventoryExportStep,
    canSelectStep: Boolean,
    onSelectStep: (InventoryExportStep) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
            .padding(bottom = 4.dp),
    ) {
        InventoryExportStep.entries.forEach { step ->
            val isReached = step <= currentStep
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .selectable(
                        selected = step == currentStep,
                        enabled = canSelectStep && step < currentStep,
                        role = Role.Tab,
                        onClick = { onSelectStep(step) },
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isReached) AppTheme.colors.primary else AppTheme.colors.surfaceVariant),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(step.labelRes),
                    style = AppTheme.typography.labelMedium,
                    color = if (step == currentStep) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
private fun InventoryExportScaffoldPreview() {
    ApkAnalyzerTheme {
        InventoryExportScaffold(
            currentStep = InventoryExportStep.Columns,
            onBack = {},
            onSelectStep = {},
            bottomBar = {
                Button(
                    text = stringResource(R.string.inventory_export_next),
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            Text(
                text = stringResource(R.string.inventory_export_no_columns_hint),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(StepContentPadding),
            )
        }
    }
}
