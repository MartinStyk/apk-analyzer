package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.icons.ApkAnalyzerIcons
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme

@Composable
fun SwipeDismiss(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SwipeToDismissBox(
        state = rememberSwipeToDismissBoxState(),
        onDismiss = { onDismiss() },
        backgroundContent = {},
        modifier = modifier.semantics {
            dismiss {
                onDismiss()
                true
            }
        },
    ) {
        content()
    }
}

@Preview
@Composable
private fun SwipeDismissPreview() {
    ApkAnalyzerTheme {
        SwipeDismiss(onDismiss = {}, modifier = Modifier.padding(16.dp)) {
            InfoCard(
                icon = ApkAnalyzerIcons.Info,
                title = null,
                body = "JSON-only details include full records, not just counts.",
            )
        }
    }
}

@Preview
@Composable
private fun SwipeDismissDarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        SwipeDismiss(onDismiss = {}, modifier = Modifier.padding(16.dp)) {
            InfoCard(
                icon = ApkAnalyzerIcons.Info,
                title = null,
                body = "JSON-only details include full records, not just counts.",
            )
        }
    }
}
