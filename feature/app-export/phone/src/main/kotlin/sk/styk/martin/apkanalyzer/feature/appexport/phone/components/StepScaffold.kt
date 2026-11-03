package sk.styk.martin.apkanalyzer.feature.appexport.phone.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Button
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Toolbar
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.feature.appexport.phone.InventoryExportStep
import sk.styk.martin.apkanalyzer.feature.appexport.phone.R

private const val STEP_ANIMATION_MILLIS = 400
private val TrackHeight = 6.dp

@Composable
internal fun StepScaffold(
    currentStep: InventoryExportStep,
    steps: List<InventoryExportStep>,
    canSelectStep: Boolean,
    onBack: () -> Unit,
    onSelectStep: (InventoryExportStep) -> Unit,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    val isCompactHeight = rememberIsCompactHeight()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        Toolbar(
            title = stringResource(R.string.inventory_export_title),
            onBack = onBack,
            actions = actions,
        )
        if (!isCompactHeight) {
            StepIndicator(
                currentStep = currentStep,
                steps = steps,
                canSelectStep = canSelectStep,
                onSelectStep = onSelectStep,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
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
                .padding(horizontal = 16.dp, vertical = if (isCompactHeight) 8.dp else 12.dp)
                .navigationBarsPadding(),
        ) {
            bottomBar()
        }
    }
}

@Composable
internal fun StepIndicator(
    currentStep: InventoryExportStep,
    steps: List<InventoryExportStep>,
    canSelectStep: Boolean,
    onSelectStep: (InventoryExportStep) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill by animateFloatAsState(
        targetValue = (steps.indexOf(currentStep) + 1f) / steps.size,
        animationSpec = tween(durationMillis = STEP_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
        label = "inventory-export-step-fill",
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(TrackHeight)
                .clip(CircleShape)
                .background(AppTheme.colors.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(AppTheme.colors.primary),
            )
        }
        Row(modifier = Modifier.fillMaxWidth().selectableGroup()) {
            steps.forEach { step ->
                StepLabel(
                    step = step,
                    currentStep = currentStep,
                    enabled = canSelectStep && step < currentStep,
                    onSelect = { onSelectStep(step) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StepLabel(
    step: InventoryExportStep,
    currentStep: InventoryExportStep,
    enabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val labelColor by animateColorAsState(
        targetValue = when {
            step == currentStep -> AppTheme.colors.onSurface
            step < currentStep -> AppTheme.colors.primary
            else -> AppTheme.colors.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = STEP_ANIMATION_MILLIS),
        label = "inventory-export-step-label",
    )
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(8.dp))
            .selectable(
                selected = step == currentStep,
                enabled = enabled,
                role = Role.Tab,
                onClick = onSelect,
            )
            .padding(top = 8.dp, bottom = 8.dp, end = 4.dp),
    ) {
        Text(
            text = stringResource(step.labelRes()),
            style = AppTheme.typography.labelMedium,
            color = labelColor,
        )
    }
}

@Preview
@Composable
private fun StepScaffoldPreview() {
    ApkAnalyzerTheme {
        StepScaffold(
            currentStep = InventoryExportStep.Columns,
            steps = InventoryExportStep.entries,
            canSelectStep = true,
            onBack = {},
            onSelectStep = {},
            bottomBar = {
                Button(text = stringResource(R.string.inventory_export_next), onClick = {}, modifier = Modifier.fillMaxWidth())
            },
        ) {}
    }
}

private val CompactHeight = 480.dp

@Composable
internal fun rememberIsCompactHeight(): Boolean {
    val windowHeight = LocalWindowInfo.current.containerSize.height
    return with(LocalDensity.current) { windowHeight.toDp() } < CompactHeight
}

@Composable
internal fun CompactStepIndicator(
    currentStep: InventoryExportStep,
    steps: List<InventoryExportStep>,
    canSelectStep: Boolean,
    onSelectStep: (InventoryExportStep) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
) {
    if (rememberIsCompactHeight()) {
        StepIndicator(
            currentStep = currentStep,
            steps = steps,
            canSelectStep = canSelectStep,
            onSelectStep = onSelectStep,
            modifier = modifier.padding(horizontal = horizontalPadding),
        )
    }
}
